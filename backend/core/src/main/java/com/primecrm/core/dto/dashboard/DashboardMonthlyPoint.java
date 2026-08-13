package com.primecrm.core.dto.dashboard;

import java.math.BigDecimal;

public record DashboardMonthlyPoint(
        String month,
        long openedCount,
        BigDecimal openedAmount,
        long wonCount,
        BigDecimal wonAmount
) {
}
