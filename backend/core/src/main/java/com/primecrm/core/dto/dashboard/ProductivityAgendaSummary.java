package com.primecrm.core.dto.dashboard;

public record ProductivityAgendaSummary(
        long scheduledToday,
        long scheduledThisWeek,
        long overdue
) {
}
