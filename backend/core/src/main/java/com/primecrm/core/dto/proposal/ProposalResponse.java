package com.primecrm.core.dto.proposal;

import com.primecrm.core.dto.common.ContactSummaryResponse;
import com.primecrm.core.dto.common.CustomerSummaryResponse;
import com.primecrm.core.dto.common.OpportunitySummaryResponse;
import com.primecrm.core.dto.common.UserSummaryResponse;
import com.primecrm.infra.entity.proposal.ProposalStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProposalResponse(
        UUID id,
        String code,
        CustomerSummaryResponse customer,
        ContactSummaryResponse contact,
        OpportunitySummaryResponse opportunity,
        UserSummaryResponse owner,
        ProposalStatus status,
        LocalDate issueDate,
        LocalDate validUntil,
        String notes,
        BigDecimal totalAmount,
        boolean expired,
        Instant decidedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
