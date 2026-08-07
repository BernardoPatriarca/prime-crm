package com.primecrm.core.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.primecrm.infra.entity.audit.AuditAction;
import com.primecrm.infra.entity.audit.AuditLog;
import com.primecrm.shared.util.TenantContext;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditLogWriterTest {

    @Mock
    private AuditLogPersister auditLogPersister;

    private AuditLogWriter auditLogWriter;

    @BeforeEach
    void setUp() {
        auditLogWriter = new AuditLogWriter(auditLogPersister);
    }

    private AuditLog capturePersistedLog() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogPersister).persist(captor.capture());
        return captor.getValue();
    }

    @Test
    void write_mapsEveryEntryFieldOntoAuditLog() {
        UUID entityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Map<String, Object> changes = Map.of("name", Map.of("old", "A", "new", "B"));

        auditLogWriter.write(new AuditEntry("Pipeline", entityId, AuditAction.UPDATE, changes, userId,
                "admin@primecrm.local", "203.0.113.7", "Mozilla/5.0", tenantId));

        AuditLog auditLog = capturePersistedLog();
        assertThat(auditLog.getEntityName()).isEqualTo("Pipeline");
        assertThat(auditLog.getEntityId()).isEqualTo(entityId);
        assertThat(auditLog.getAction()).isEqualTo(AuditAction.UPDATE);
        assertThat(auditLog.getChanges()).isEqualTo(changes);
        assertThat(auditLog.getUserId()).isEqualTo(userId);
        assertThat(auditLog.getUserEmail()).isEqualTo("admin@primecrm.local");
        assertThat(auditLog.getIpAddress()).isEqualTo("203.0.113.7");
        assertThat(auditLog.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(auditLog.getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void write_withoutTenant_fallsBackToCurrentTenant() {
        auditLogWriter.write(new AuditEntry("Role", UUID.randomUUID(), AuditAction.CREATE, Map.of(), null, null,
                null, null, null));

        assertThat(capturePersistedLog().getTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void write_withOversizedUserAgent_truncatesToColumnLength() {
        String longUserAgent = "u".repeat(400);

        auditLogWriter.write(new AuditEntry("User", UUID.randomUUID(), AuditAction.CREATE, Map.of(), null, null,
                null, longUserAgent, null));

        assertThat(capturePersistedLog().getUserAgent()).hasSize(255);
    }

    @Test
    void write_whenPersistenceFails_doesNotPropagateException() {
        doThrow(new IllegalStateException("banco indisponivel")).when(auditLogPersister).persist(any(AuditLog.class));

        assertThatCode(() -> auditLogWriter.write(new AuditEntry("User", UUID.randomUUID(), AuditAction.DELETE,
                Map.of(), null, null, null, null, null)))
                .doesNotThrowAnyException();
    }
}
