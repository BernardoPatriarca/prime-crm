package com.primecrm.core.dto.common;

import java.util.UUID;

public record DomainValueSummaryResponse(
        UUID id,
        String code,
        String name,
        String color
) {
}
