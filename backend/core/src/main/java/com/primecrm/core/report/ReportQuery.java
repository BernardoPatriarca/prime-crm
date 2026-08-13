package com.primecrm.core.report;

import org.springframework.data.jpa.domain.Specification;

public record ReportQuery<T>(
        String report,
        String groupBy,
        Class<T> entityType,
        ReportDimension<T> dimension,
        String measureAttribute,
        Specification<T> filter
) {

    public boolean measured() {
        return measureAttribute != null;
    }
}
