package com.primecrm.core.dto.dashboard;

public record DashboardTaskSummary(
        long pending,
        long inProgress,
        long overdue,
        long dueToday,
        long completedThisWeek
) {
}
