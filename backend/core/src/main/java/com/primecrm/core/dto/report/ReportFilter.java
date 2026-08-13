package com.primecrm.core.dto.report;

import java.time.Instant;
import java.util.UUID;

public record ReportFilter(
        Instant from,
        Instant to,
        UUID userId
) {

    public static ReportFilter empty() {
        return new ReportFilter(null, null, null);
    }
}
