package com.primecrm.core.dto.commercial;

import com.primecrm.core.dto.common.ContactSummaryResponse;
import com.primecrm.core.dto.common.CustomerSummaryResponse;
import com.primecrm.core.dto.common.DomainValueSummaryResponse;
import com.primecrm.core.dto.common.LeadSummaryResponse;
import com.primecrm.core.dto.common.PipelineStageSummaryResponse;
import com.primecrm.core.dto.common.PipelineSummaryResponse;
import com.primecrm.core.dto.common.UserSummaryResponse;
import com.primecrm.infra.entity.commercial.OpportunityOutcome;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;

@Builder
public record OpportunityResponse(
        UUID id,
        String code,
        String title,
        CustomerSummaryResponse customer,
        ContactSummaryResponse contact,
        PipelineSummaryResponse pipeline,
        PipelineStageSummaryResponse stage,
        BigDecimal amount,
        BigDecimal probability,
        UserSummaryResponse owner,
        DomainValueSummaryResponse team,
        Instant openedAt,
        LocalDate expectedCloseDate,
        Instant closedAt,
        OpportunityOutcome outcome,
        DomainValueSummaryResponse winReason,
        DomainValueSummaryResponse lossReason,
        String competitor,
        LeadSummaryResponse sourceLead,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
