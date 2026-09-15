package com.primecrm.core.dto.dashboard;

import java.math.BigDecimal;

public record ContractDashboardMetrics(
        long activeCount,
        BigDecimal activeRecurringAmount,
        long expiringCount,
        BigDecimal expiringAmount
) {
}
