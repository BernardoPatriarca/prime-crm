package com.primecrm.core.dto.order;

import com.primecrm.core.dto.common.CustomerSummaryResponse;
import com.primecrm.core.dto.common.OpportunitySummaryResponse;
import com.primecrm.core.dto.common.ProposalSummaryResponse;
import com.primecrm.core.dto.common.UserSummaryResponse;
import com.primecrm.infra.entity.order.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String code,
        CustomerSummaryResponse customer,
        ProposalSummaryResponse proposal,
        OpportunitySummaryResponse opportunity,
        UserSummaryResponse owner,
        OrderStatus status,
        LocalDate orderDate,
        LocalDate deliveryDate,
        String notes,
        BigDecimal totalAmount,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
