package com.primecrm.core.dto.finance;

import com.primecrm.infra.entity.finance.PayableStatus;
import java.time.LocalDate;
import java.util.UUID;

public record PayableListFilter(
        String search,
        PayableStatus status,
        UUID supplierId,
        UUID categoryId,
        LocalDate dueFrom,
        LocalDate dueTo,
        Boolean overdue
) {
}
