package com.primecrm.core.dto.finance;

import com.primecrm.infra.entity.finance.ReceivableStatus;
import java.time.LocalDate;
import java.util.UUID;

public record ReceivableListFilter(
        String search,
        ReceivableStatus status,
        UUID customerId,
        UUID orderId,
        UUID contractId,
        LocalDate dueFrom,
        LocalDate dueTo,
        Boolean overdue
) {
}
