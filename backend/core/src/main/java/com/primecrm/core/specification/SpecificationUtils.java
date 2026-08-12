package com.primecrm.core.specification;

import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public final class SpecificationUtils {

    private SpecificationUtils() {
    }

    @SafeVarargs
    public static <T> Specification<T> and(Specification<T>... specs) {
        Specification<T> result = Specification.where(null);
        for (Specification<T> spec : specs) {
            if (spec != null) {
                result = result.and(spec);
            }
        }
        return result;
    }

    public static <T> Specification<T> fetchToOne(String... paths) {
        return (root, query, cb) -> {
            if (isCountQuery(query)) {
                return null;
            }
            for (String path : paths) {
                root.fetch(path, JoinType.LEFT);
            }
            return null;
        };
    }

    public static boolean isCountQuery(jakarta.persistence.criteria.CriteriaQuery<?> query) {
        if (query == null) {
            return false;
        }
        Class<?> resultType = query.getResultType();
        return Long.class.equals(resultType) || long.class.equals(resultType);
    }
}
