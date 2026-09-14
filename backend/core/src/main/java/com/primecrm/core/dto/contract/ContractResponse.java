package com.primecrm.core.dto.contract;

import com.primecrm.core.dto.common.CustomerSummaryResponse;
import com.primecrm.core.dto.common.DomainValueSummaryResponse;
import com.primecrm.core.dto.common.OpportunitySummaryResponse;
import com.primecrm.core.dto.common.OrderSummaryResponse;
import com.primecrm.core.dto.common.UserSummaryResponse;
import com.primecrm.infra.entity.contract.ContractStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ContractResponse(
        UUID id,
        String code,
        CustomerSummaryResponse customer,
        OrderSummaryResponse order,
        OpportunitySummaryResponse opportunity,
        UserSummaryResponse owner,
        DomainValueSummaryResponse billingCycle,
        ContractStatus status,
        LocalDate startDate,
        LocalDate endDate,
        boolean autoRenew,
        BigDecimal recurringAmount,
        String notes,
        boolean expired,
        Instant terminatedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
