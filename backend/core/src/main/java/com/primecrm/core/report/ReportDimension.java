package com.primecrm.core.report;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

@FunctionalInterface
public interface ReportDimension<T> {

    String MONTH_FORMAT = "YYYY-MM";

    Expression<?> expression(Root<T> root, CriteriaBuilder cb);

    static <T> ReportDimension<T> attribute(String attribute) {
        return (root, cb) -> root.get(attribute);
    }

    static <T> ReportDimension<T> referenceName(String reference) {
        return (root, cb) -> root.join(reference, JoinType.LEFT).get("name");
    }

    static <T> ReportDimension<T> month(String attribute) {
        return (root, cb) -> {
            Path<?> path = root.get(attribute);
            return cb.function("to_char", String.class, path, cb.literal(MONTH_FORMAT));
        };
    }
}
