package com.primecrm.core.dto.domain;

import java.util.UUID;

public record DomainTypeResponse(
        UUID id,
        String code,
        String label,
        boolean supportsColor,
        boolean supportsIcon,
        boolean systemDefined
) {
}
