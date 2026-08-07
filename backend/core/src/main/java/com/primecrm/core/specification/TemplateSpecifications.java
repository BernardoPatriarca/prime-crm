package com.primecrm.core.specification;

import com.primecrm.infra.entity.config.Template;
import com.primecrm.infra.entity.config.TemplateType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class TemplateSpecifications {

    private TemplateSpecifications() {
    }

    public static Specification<Template> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Template> hasType(TemplateType type) {
        if (type == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<Template> hasActive(Boolean active) {
        if (active == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }

    public static Specification<Template> textSearch(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            Predicate byName = cb.like(cb.lower(root.get("name")), pattern);
            Predicate bySubject = cb.like(cb.lower(root.get("subject")), pattern);
            return cb.or(byName, bySubject);
        };
    }
}
