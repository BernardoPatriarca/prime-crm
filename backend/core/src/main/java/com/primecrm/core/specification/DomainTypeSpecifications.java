package com.primecrm.core.specification;

import com.primecrm.infra.entity.domain.DomainType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class DomainTypeSpecifications {

    private DomainTypeSpecifications() {
    }

    public static Specification<DomainType> byCode(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("code"), code.trim());
    }
}
