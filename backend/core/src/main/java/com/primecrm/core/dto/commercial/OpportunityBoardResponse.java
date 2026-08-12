package com.primecrm.core.dto.commercial;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OpportunityBoardResponse(
        UUID pipelineId,
        String pipelineName,
        int limitPerStage,
        long totalCount,
        BigDecimal totalAmount,
        List<OpportunityBoardColumnResponse> columns
) {
}
