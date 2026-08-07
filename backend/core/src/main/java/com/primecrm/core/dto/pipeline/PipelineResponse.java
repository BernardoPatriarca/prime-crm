package com.primecrm.core.dto.pipeline;

import java.util.List;
import java.util.UUID;

public record PipelineResponse(
        UUID id,
        String name,
        String businessType,
        boolean active,
        List<PipelineStageResponse> stages
) {
}
