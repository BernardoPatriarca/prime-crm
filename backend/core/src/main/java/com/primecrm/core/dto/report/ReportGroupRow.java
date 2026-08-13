package com.primecrm.core.dto.report;

import java.math.BigDecimal;

public record ReportGroupRow(
        String label,
        long count,
        BigDecimal total,
        BigDecimal percentage
) {
}
