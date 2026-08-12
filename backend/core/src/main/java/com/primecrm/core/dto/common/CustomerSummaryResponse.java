package com.primecrm.core.dto.common;

import java.util.UUID;

public record CustomerSummaryResponse(
        UUID id,
        String code,
        String name
) {
}
