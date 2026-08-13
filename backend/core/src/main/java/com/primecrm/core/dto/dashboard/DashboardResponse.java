package com.primecrm.core.dto.dashboard;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(
        LocalDate from,
        LocalDate to,
        Instant generatedAt,
        DashboardMetrics metrics,
        DashboardFunnel funnel,
        List<DashboardMonthlyPoint> monthly,
        List<DashboardRankingRow> ranking,
        DashboardTaskSummary tasks
) {
}
