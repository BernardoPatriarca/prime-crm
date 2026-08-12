package com.primecrm.core.dto.common;

import java.math.BigDecimal;
import java.util.UUID;

public record PipelineStageSummaryResponse(
        UUID id,
        String name,
        int displayOrder,
        BigDecimal defaultProbability,
        String color,
        boolean requiresLossReason
) {
}
