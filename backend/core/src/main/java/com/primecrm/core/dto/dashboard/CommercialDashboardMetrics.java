package com.primecrm.core.dto.dashboard;

import java.math.BigDecimal;

public record CommercialDashboardMetrics(
        long totalCount,
        BigDecimal totalAmount,
        long closedCount,
        BigDecimal closedAmount,
        BigDecimal conversionRate
) {
}
