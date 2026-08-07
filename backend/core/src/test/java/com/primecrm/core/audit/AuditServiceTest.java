package com.primecrm.core.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.security.AuthenticatedUser;
import com.primecrm.infra.entity.audit.AuditAction;
import com.primecrm.infra.entity.auth.User;
import com.primecrm.infra.entity.auth.UserStatus;
import com.primecrm.infra.entity.config.Holiday;
import com.primecrm.shared.util.TenantContext;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private ObjectProvider<AuditRequestContext> requestContextProvider;
    @Mock
    private AuditRequestContext requestContext;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(eventPublisher, requestContextProvider);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private AuditEntry capturePublishedEntry() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(AuditEntry.class);
        return (AuditEntry) captor.getValue();
    }

    private User sampleUser(UUID id) {
        User user = new User();
        user.setId(id);
        user.setName("Maria Silva");
        user.setEmail("maria@primecrm.local");
        user.setLogin("maria");
        user.setPasswordHash("$2a$10$superSecretHashValue");
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    @Test
    void recordCreate_publishesEntryWithCreateActionAndEntityIdentity() {
        UUID id = UUID.randomUUID();

        auditService.recordCreate(sampleUser(id));

        AuditEntry entry = capturePublishedEntry();
        assertThat(entry.action()).isEqualTo(AuditAction.CREATE);
        assertThat(entry.entityName()).isEqualTo("User");
        assertThat(entry.entityId()).isEqualTo(id);
        assertThat(entry.tenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
        assertThat(entry.changes()).containsEntry("name", "Maria Silva");
        assertThat(entry.changes()).containsEntry("status", "ACTIVE");
    }

    @Test
    void recordCreate_neverLeaksPasswordHashIntoChanges() {
        User user = sampleUser(UUID.randomUUID());

        auditService.recordCreate(user);

        AuditEntry entry = capturePublishedEntry();
        assertThat(entry.changes()).doesNotContainKey("passwordHash");
        assertThat(entry.changes().values().stream().map(String::valueOf))
                .noneMatch(value -> value.contains("superSecretHashValue"));
    }

    @Test
    void recordUpdate_publishesOnlyChangedFieldsWithOldAndNewValues() {
        Holiday holiday = new Holiday();
        holiday.setId(UUID.randomUUID());
        holiday.setName("Natal");
        holiday.setHolidayDate(LocalDate.of(2026, 12, 25));
        holiday.setActive(true);

        Map<String, Object> previousState = auditService.snapshot(holiday);
        holiday.setName("Natal (feriado nacional)");

        auditService.recordUpdate(holiday, previousState);

        AuditEntry entry = capturePublishedEntry();
        assertThat(entry.action()).isEqualTo(AuditAction.UPDATE);
        assertThat(entry.entityName()).isEqualTo("Holiday");
        assertThat(entry.changes()).containsOnlyKeys("name");
        assertThat(entry.changes().get("name")).isEqualTo(
                Map.of(AuditChanges.OLD_KEY, "Natal", AuditChanges.NEW_KEY, "Natal (feriado nacional)"));
    }

    @Test
    void recordDelete_publishesDeleteActionWithSnapshot() {
        Holiday holiday = new Holiday();
        holiday.setId(UUID.randomUUID());
        holiday.setName("Carnaval");

        auditService.recordDelete(holiday);

        AuditEntry entry = capturePublishedEntry();
        assertThat(entry.action()).isEqualTo(AuditAction.DELETE);
        assertThat(entry.changes()).containsEntry("name", "Carnaval");
    }

    @Test
    void recordChange_withoutAuthenticatedUser_publishesNullUserWithoutFailing() {
        UUID entityId = UUID.randomUUID();

        auditService.recordChange(AuditAction.UPDATE, "SystemSetting", entityId, Map.of());

        AuditEntry entry = capturePublishedEntry();
        assertThat(entry.userId()).isNull();
        assertThat(entry.userEmail()).isNull();
    }

    @Test
    void recordChange_withAuthenticatedUser_publishesUserIdentity() {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser principal = new AuthenticatedUser(userId, "admin@primecrm.local", "admin", "Admin",
                List.of(), List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        auditService.recordChange(AuditAction.CREATE, "Pipeline", UUID.randomUUID(), Map.of());

        AuditEntry entry = capturePublishedEntry();
        assertThat(entry.userId()).isEqualTo(userId);
        assertThat(entry.userEmail()).isEqualTo("admin@primecrm.local");
    }

    @Test
    void recordChange_withRequestContext_publishesIpAndUserAgent() {
        when(requestContextProvider.getIfAvailable()).thenReturn(requestContext);
        when(requestContext.currentIpAddress()).thenReturn("203.0.113.7");
        when(requestContext.currentUserAgent()).thenReturn("Mozilla/5.0");

        auditService.recordChange(AuditAction.CREATE, "Template", UUID.randomUUID(), Map.of());

        AuditEntry entry = capturePublishedEntry();
        assertThat(entry.ipAddress()).isEqualTo("203.0.113.7");
        assertThat(entry.userAgent()).isEqualTo("Mozilla/5.0");
    }

    @Test
    void recordChange_withoutRequestContext_publishesNullIpAndUserAgent() {
        when(requestContextProvider.getIfAvailable()).thenReturn(null);

        auditService.recordChange(AuditAction.CREATE, "Template", UUID.randomUUID(), Map.of());

        AuditEntry entry = capturePublishedEntry();
        assertThat(entry.ipAddress()).isNull();
        assertThat(entry.userAgent()).isNull();
    }

    @Test
    void recordChange_whenPublisherFails_doesNotPropagateException() {
        doThrow(new IllegalStateException("event bus indisponivel"))
                .when(eventPublisher).publishEvent(any(Object.class));

        assertThatCode(() -> auditService.recordChange(AuditAction.DELETE, "Role", UUID.randomUUID(), Map.of()))
                .doesNotThrowAnyException();
    }

    @Test
    void recordCreate_whenRequestContextFails_doesNotPropagateException() {
        when(requestContextProvider.getIfAvailable()).thenThrow(new IllegalStateException("sem request"));

        assertThatCode(() -> auditService.recordCreate(sampleUser(UUID.randomUUID())))
                .doesNotThrowAnyException();
    }
}
