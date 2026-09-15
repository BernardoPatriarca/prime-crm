package com.primecrm.core.dto.finance;

import com.primecrm.core.dto.common.ContractSummaryResponse;
import com.primecrm.core.dto.common.CustomerSummaryResponse;
import com.primecrm.core.dto.common.DomainValueSummaryResponse;
import com.primecrm.core.dto.common.OrderSummaryResponse;
import com.primecrm.infra.entity.finance.ReceivableStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReceivableResponse(
        UUID id,
        String code,
        CustomerSummaryResponse customer,
        OrderSummaryResponse order,
        ContractSummaryResponse contract,
        String description,
        int installmentNumber,
        int totalInstallments,
        LocalDate dueDate,
        BigDecimal amount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        Instant paidAt,
        DomainValueSummaryResponse paymentMethod,
        ReceivableStatus status,
        boolean overdue,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
