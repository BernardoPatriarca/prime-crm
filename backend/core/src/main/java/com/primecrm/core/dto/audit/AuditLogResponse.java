package com.primecrm.core.dto.audit;

import com.primecrm.infra.entity.audit.AuditAction;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        String entityName,
        UUID entityId,
        AuditAction action,
        Map<String, Object> changes,
        UUID userId,
        String userEmail,
        String ipAddress,
        String userAgent,
        Instant createdAt
) {
}
