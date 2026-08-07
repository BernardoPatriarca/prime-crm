package com.primecrm.core.specification;

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
}
