package com.primecrm.core.dto.dashboard;

import java.math.BigDecimal;

public record FinanceDashboardMetrics(
        long openCount,
        BigDecimal openAmount,
        long overdueCount,
        BigDecimal overdueAmount,
        long movementCount,
        BigDecimal movementAmount,
        BigDecimal movementTrend
) {
}
