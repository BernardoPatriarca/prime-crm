package com.primecrm.core.dto.sales;

import com.primecrm.core.dto.common.UserSummaryResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SalesGoalResponse(
        UUID id,
        UserSummaryResponse owner,
        LocalDate referenceMonth,
        BigDecimal targetAmount,
        BigDecimal realizedAmount,
        BigDecimal achievementPercent,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
