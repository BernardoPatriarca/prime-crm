package com.primecrm.core.dto.commercial;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OpportunityBoardColumnResponse(
        UUID stageId,
        String stageName,
        int displayOrder,
        BigDecimal defaultProbability,
        String color,
        boolean requiresLossReason,
        long totalCount,
        BigDecimal totalAmount,
        boolean hasMore,
        List<OpportunityCardResponse> opportunities
) {
}
