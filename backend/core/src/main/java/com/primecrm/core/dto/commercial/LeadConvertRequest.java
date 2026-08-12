package com.primecrm.core.dto.commercial;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LeadConvertRequest(

        UUID clientTypeId,
        UUID segmentId,
        UUID ownerUserId,

        Boolean createOpportunity,

        UUID pipelineId,
        UUID stageId,

        @Size(max = 200)
        String opportunityTitle,

        BigDecimal amount,

        LocalDate expectedCloseDate
) {
}
