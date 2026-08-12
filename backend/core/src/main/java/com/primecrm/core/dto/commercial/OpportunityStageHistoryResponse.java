package com.primecrm.core.dto.commercial;

import com.primecrm.core.dto.common.PipelineStageSummaryResponse;
import com.primecrm.core.dto.common.UserSummaryResponse;
import java.time.Instant;
import java.util.UUID;

public record OpportunityStageHistoryResponse(
        UUID id,
        PipelineStageSummaryResponse fromStage,
        PipelineStageSummaryResponse toStage,
        UserSummaryResponse movedByUser,
        Instant movedAt,
        Integer daysInPreviousStage,
        String note
) {
}
