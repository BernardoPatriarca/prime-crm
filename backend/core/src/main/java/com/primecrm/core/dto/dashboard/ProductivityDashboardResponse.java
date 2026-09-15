package com.primecrm.core.dto.dashboard;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ProductivityDashboardResponse(
        LocalDate from,
        LocalDate to,
        Instant generatedAt,
        DashboardTaskSummary tasks,
        List<ProductivityRankingRow> taskRanking,
        ProductivityAgendaSummary agenda
) {
}
