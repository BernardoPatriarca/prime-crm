package com.primecrm.core.audit;

import com.primecrm.infra.entity.audit.AuditAction;
import java.util.Map;
import java.util.UUID;

public record AuditEntry(
        String entityName,
        UUID entityId,
        AuditAction action,
        Map<String, Object> changes,
        UUID userId,
        String userEmail,
        String ipAddress,
        String userAgent,
        UUID tenantId
) {
}
