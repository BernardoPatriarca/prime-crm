package com.primecrm.core.dto.common;

import java.util.UUID;

public record ProductSummaryResponse(
        UUID id,
        String code,
        String name
) {
}
