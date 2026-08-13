package com.primecrm.core.dto.dashboard;

import java.math.BigDecimal;

public record DashboardMetrics(
        long newLeads,
        BigDecimal newLeadsTrend,
        long convertedLeads,
        BigDecimal leadConversionRate,
        long openOpportunities,
        BigDecimal openAmount,
        long wonOpportunities,
        BigDecimal wonAmount,
        BigDecimal wonAmountTrend,
        long lostOpportunities,
        BigDecimal winRate,
        BigDecimal averageTicket,
        long activeCustomers,
        long newCustomers,
        BigDecimal newCustomersTrend
) {
}
