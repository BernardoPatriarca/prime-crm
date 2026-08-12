package com.primecrm.core.dto.commercial;

import java.time.LocalDate;
import java.util.UUID;

public record LeadListFilter(
        String search,
        UUID originId,
        UUID statusId,
        UUID priorityId,
        UUID ownerUserId,
        UUID pipelineId,
        UUID stageId,
        Boolean active,
        LocalDate expectedCloseFrom,
        LocalDate expectedCloseTo
) {
}
