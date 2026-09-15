package com.primecrm.core.dto.dashboard;

import java.math.BigDecimal;

public record CommercialMonthlyPoint(
        String month,
        BigDecimal proposalsAmount,
        BigDecimal ordersAmount
) {
}
