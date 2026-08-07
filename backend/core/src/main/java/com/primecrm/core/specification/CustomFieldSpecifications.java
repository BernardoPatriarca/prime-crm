package com.primecrm.core.specification;

import com.primecrm.infra.entity.config.CustomField;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class CustomFieldSpecifications {

    private CustomFieldSpecifications() {
    }

    public static Specification<CustomField> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<CustomField> byTargetEntity(String targetEntity) {
        if (!StringUtils.hasText(targetEntity)) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("targetEntity"), targetEntity.trim());
    }

    public static Specification<CustomField> hasActive(Boolean active) {
        if (active == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }

    public static Specification<CustomField> textSearch(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            Predicate byLabel = cb.like(cb.lower(root.get("label")), pattern);
            Predicate byFieldKey = cb.like(cb.lower(root.get("fieldKey")), pattern);
            return cb.or(byLabel, byFieldKey);
        };
    }
}
