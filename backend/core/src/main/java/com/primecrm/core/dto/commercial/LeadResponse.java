package com.primecrm.core.dto.commercial;

import com.primecrm.core.dto.common.CustomerSummaryResponse;
import com.primecrm.core.dto.common.DomainValueSummaryResponse;
import com.primecrm.core.dto.common.PipelineStageSummaryResponse;
import com.primecrm.core.dto.common.PipelineSummaryResponse;
import com.primecrm.core.dto.common.UserSummaryResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record LeadResponse(
        UUID id,
        String code,
        String name,
        String companyName,
        String contactName,
        String email,
        String phone,
        String mobile,
        DomainValueSummaryResponse origin,
        DomainValueSummaryResponse status,
        DomainValueSummaryResponse priority,
        String campaign,
        UserSummaryResponse owner,
        PipelineSummaryResponse pipeline,
        PipelineStageSummaryResponse stage,
        BigDecimal probability,
        BigDecimal estimatedValue,
        LocalDate expectedCloseDate,
        Integer qualificationScore,
        CustomerSummaryResponse convertedCustomer,
        Instant convertedAt,
        String notes,
        boolean active,
        List<DomainValueSummaryResponse> tags,
        Instant createdAt,
        Instant updatedAt
) {
}
