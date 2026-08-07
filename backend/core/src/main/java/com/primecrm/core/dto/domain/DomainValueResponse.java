package com.primecrm.core.dto.domain;

import java.util.Map;
import java.util.UUID;

public record DomainValueResponse(
        UUID id,
        String domainTypeCode,
        String domainTypeLabel,
        String code,
        String name,
        String description,
        String color,
        String icon,
        int displayOrder,
        Map<String, Object> extra,
        boolean active
) {
}
