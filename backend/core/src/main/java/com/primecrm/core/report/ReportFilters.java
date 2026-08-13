package com.primecrm.core.report;

import com.primecrm.core.dto.report.ReportFilter;
import com.primecrm.core.specification.SpecificationUtils;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class ReportFilters {

    private ReportFilters() {
    }

    public static <T> Specification<T> of(ReportFilter filter, String dateAttribute, String userAttribute) {
        return SpecificationUtils.and(
                notDeleted(),
                from(dateAttribute, filter.from()),
                to(dateAttribute, filter.to()),
                byReference(userAttribute, filter.userId()));
    }

    public static <T> Specification<T> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    private static <T> Specification<T> from(String attribute, Instant from) {
        if (from == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(attribute), from);
    }

    private static <T> Specification<T> to(String attribute, Instant to) {
        if (to == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get(attribute), to);
    }

    private static <T> Specification<T> byReference(String attribute, UUID id) {
        if (id == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get(attribute).get("id"), id);
    }
}
