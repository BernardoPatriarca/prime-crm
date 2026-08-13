package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.audit.AuditLogFilter;
import com.primecrm.core.dto.audit.AuditLogResponse;
import com.primecrm.core.mapper.AuditLogMapper;
import com.primecrm.infra.entity.audit.AuditAction;
import com.primecrm.infra.entity.audit.AuditLog;
import com.primecrm.infra.repository.AuditLogRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private AuditLogMapper auditLogMapper;
    @Mock
    private AuditService auditService;

    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogService(auditLogRepository, auditLogMapper, auditService);
    }

    @Test
    void list_mapsTheEntriesToResponses() {
        AuditLog entry = newEntry();
        when(auditLogRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(entry)));
        when(auditLogMapper.toResponse(entry)).thenReturn(toResponse(entry));

        var page = auditLogService.list(emptyFilter(), PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().entityName()).isEqualTo("Customer");
    }

    @Test
    void export_registersItselfInTheAuditTrail() {
        AuditLog entry = newEntry();
        when(auditLogRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(entry)));
        when(auditLogMapper.toResponse(entry)).thenReturn(toResponse(entry));

        assertThat(auditLogService.export(emptyFilter())).hasSize(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(auditService).recordChange(eq(AuditAction.EXPORT), eq("AuditLog"), isNull(), captor.capture());
        assertThat(captor.getValue()).containsEntry("rows", 1);
    }

    @Test
    void entityNames_delegatesToTheRepository() {
        when(auditLogRepository.findDistinctEntityNames()).thenReturn(List.of("Customer", "Task"));

        assertThat(auditLogService.entityNames()).containsExactly("Customer", "Task");
    }

    private AuditLogFilter emptyFilter() {
        return new AuditLogFilter(null, null, null, null, null, null, null);
    }

    private AuditLog newEntry() {
        AuditLog entry = new AuditLog();
        entry.setId(UUID.randomUUID());
        entry.setEntityName("Customer");
        entry.setEntityId(UUID.randomUUID());
        entry.setAction(AuditAction.UPDATE);
        entry.setCreatedAt(Instant.now());
        return entry;
    }

    private AuditLogResponse toResponse(AuditLog entry) {
        return new AuditLogResponse(entry.getId(), entry.getEntityName(), entry.getEntityId(), entry.getAction(),
                Map.of(), null, null, null, null, entry.getCreatedAt());
    }
}
