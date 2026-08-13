package com.primecrm.core.dto.report;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ReportResponse(
        String report,
        String groupBy,
        boolean measured,
        long totalCount,
        BigDecimal totalAmount,
        Instant generatedAt,
        List<ReportGroupRow> rows
) {
}
