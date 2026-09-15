package com.primecrm.core.dto.dashboard;

import java.math.BigDecimal;

public record FinanceMonthlyPoint(
        String month,
        BigDecimal receivedAmount,
        BigDecimal paidAmount,
        BigDecimal netAmount
) {
}
