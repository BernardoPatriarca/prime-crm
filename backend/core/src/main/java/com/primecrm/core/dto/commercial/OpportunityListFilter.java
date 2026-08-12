package com.primecrm.core.dto.commercial;

import com.primecrm.infra.entity.commercial.OpportunityOutcome;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record OpportunityListFilter(
        String search,
        UUID pipelineId,
        UUID stageId,
        UUID customerId,
        UUID ownerUserId,
        OpportunityOutcome outcome,
        LocalDate expectedCloseFrom,
        LocalDate expectedCloseTo,
        BigDecimal amountFrom,
        BigDecimal amountTo
) {
}
