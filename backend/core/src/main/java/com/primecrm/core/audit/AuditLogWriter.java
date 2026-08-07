package com.primecrm.core.audit;

import com.primecrm.infra.entity.audit.AuditLog;
import com.primecrm.shared.util.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogWriter {

    private static final int ENTITY_NAME_MAX_LENGTH = 120;
    private static final int USER_EMAIL_MAX_LENGTH = 180;
    private static final int IP_ADDRESS_MAX_LENGTH = 64;
    private static final int USER_AGENT_MAX_LENGTH = 255;

    private final AuditLogPersister auditLogPersister;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void write(AuditEntry entry) {
        try {
            auditLogPersister.persist(toAuditLog(entry));
        } catch (RuntimeException ex) {
            log.error("Falha ao gravar audit_log {} de {} [{}]", entry.action(), entry.entityName(),
                    entry.entityId(), ex);
        }
    }

    private AuditLog toAuditLog(AuditEntry entry) {
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityName(truncate(entry.entityName(), ENTITY_NAME_MAX_LENGTH));
        auditLog.setEntityId(entry.entityId());
        auditLog.setAction(entry.action());
        auditLog.setChanges(entry.changes());
        auditLog.setUserId(entry.userId());
        auditLog.setUserEmail(truncate(entry.userEmail(), USER_EMAIL_MAX_LENGTH));
        auditLog.setIpAddress(truncate(entry.ipAddress(), IP_ADDRESS_MAX_LENGTH));
        auditLog.setUserAgent(truncate(entry.userAgent(), USER_AGENT_MAX_LENGTH));
        auditLog.setTenantId(entry.tenantId() == null ? TenantContext.getCurrentTenant() : entry.tenantId());
        return auditLog;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
