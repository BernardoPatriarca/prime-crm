package com.primecrm.core.dto.finance;

import com.primecrm.core.dto.common.CustomerSummaryResponse;
import com.primecrm.core.dto.common.DomainValueSummaryResponse;
import com.primecrm.infra.entity.finance.PayableStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PayableResponse(
        UUID id,
        String code,
        CustomerSummaryResponse supplier,
        DomainValueSummaryResponse category,
        String description,
        LocalDate dueDate,
        BigDecimal amount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        Instant paidAt,
        DomainValueSummaryResponse paymentMethod,
        PayableStatus status,
        boolean overdue,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
