package com.primecrm.core.specification;

import com.primecrm.infra.entity.domain.DomainValue;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class DomainValueSpecifications {

    private DomainValueSpecifications() {
    }

    public static Specification<DomainValue> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<DomainValue> byDomainTypeCode(String domainTypeCode) {
        if (!StringUtils.hasText(domainTypeCode)) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("domainType").get("code"), domainTypeCode.trim());
    }

    public static Specification<DomainValue> hasActive(Boolean active) {
        if (active == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }

    public static Specification<DomainValue> textSearch(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            Predicate byName = cb.like(cb.lower(root.get("name")), pattern);
            Predicate byCode = cb.like(cb.lower(root.get("code")), pattern);
            Predicate byDescription = cb.like(cb.lower(root.get("description")), pattern);
            return cb.or(byName, byCode, byDescription);
        };
    }
}
