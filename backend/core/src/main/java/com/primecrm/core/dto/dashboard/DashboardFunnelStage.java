package com.primecrm.core.dto.dashboard;

import java.math.BigDecimal;

public record DashboardFunnelStage(
        String name,
        String color,
        int displayOrder,
        long count,
        BigDecimal amount
) {
}
