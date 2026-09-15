package com.primecrm.core.dto.dashboard;

import java.math.BigDecimal;

public record ProductivityRankingRow(
        String owner,
        long completedCount,
        BigDecimal share
) {
}
