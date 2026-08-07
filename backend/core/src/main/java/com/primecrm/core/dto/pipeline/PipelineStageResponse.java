package com.primecrm.core.dto.pipeline;

import java.math.BigDecimal;
import java.util.UUID;

public record PipelineStageResponse(
        UUID id,
        UUID pipelineId,
        String name,
        int displayOrder,
        BigDecimal defaultProbability,
        Integer slaDays,
        String color,
        boolean requiresLossReason
) {
}
