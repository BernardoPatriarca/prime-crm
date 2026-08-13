package com.primecrm.core.dto.dashboard;

import java.math.BigDecimal;

public record DashboardRankingRow(
        String owner,
        long count,
        BigDecimal amount,
        BigDecimal share
) {
}
