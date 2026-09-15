package com.primecrm.core.dto.sales;

import java.time.LocalDate;
import java.util.UUID;

public record SalesGoalListFilter(
        String search,
        UUID ownerUserId,
        LocalDate referenceMonth
) {
}
