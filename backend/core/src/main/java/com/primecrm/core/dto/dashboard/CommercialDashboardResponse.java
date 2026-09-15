package com.primecrm.core.dto.dashboard;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record CommercialDashboardResponse(
        LocalDate from,
        LocalDate to,
        Instant generatedAt,
        CommercialDashboardMetrics proposals,
        CommercialDashboardMetrics orders,
        ContractDashboardMetrics contracts,
        List<CommercialMonthlyPoint> monthly
) {
}
