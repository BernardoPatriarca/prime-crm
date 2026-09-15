package com.primecrm.core.dto.dashboard;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record FinanceDashboardResponse(
        LocalDate from,
        LocalDate to,
        Instant generatedAt,
        FinanceDashboardMetrics receivables,
        FinanceDashboardMetrics payables,
        List<FinanceMonthlyPoint> monthly
) {
}
