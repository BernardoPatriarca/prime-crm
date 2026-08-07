package com.primecrm.core.specification;

import com.primecrm.infra.entity.config.Pipeline;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class PipelineSpecifications {

    private PipelineSpecifications() {
    }

    public static Specification<Pipeline> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Pipeline> hasActive(Boolean active) {
        if (active == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }

    public static Specification<Pipeline> textSearch(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            Predicate byName = cb.like(cb.lower(root.get("name")), pattern);
            Predicate byBusinessType = cb.like(cb.lower(root.get("businessType")), pattern);
            return cb.or(byName, byBusinessType);
        };
    }
}
