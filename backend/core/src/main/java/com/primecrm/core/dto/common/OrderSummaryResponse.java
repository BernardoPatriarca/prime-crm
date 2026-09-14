package com.primecrm.core.dto.common;

import java.util.UUID;

public record OrderSummaryResponse(
        UUID id,
        String code
) {
}
