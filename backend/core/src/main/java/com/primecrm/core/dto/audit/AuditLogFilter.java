package com.primecrm.core.dto.audit;

import com.primecrm.infra.entity.audit.AuditAction;
import java.time.Instant;
import java.util.UUID;

public record AuditLogFilter(
        String search,
        String entityName,
        UUID entityId,
        AuditAction action,
        UUID userId,
        Instant from,
        Instant to
) {
}
