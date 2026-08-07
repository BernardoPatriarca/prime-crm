package com.primecrm.core.specification;

import com.primecrm.infra.entity.auth.RefreshToken;
import org.springframework.data.jpa.domain.Specification;

public final class RefreshTokenSpecifications {

    private RefreshTokenSpecifications() {
    }

    public static Specification<RefreshToken> byTokenHash(String tokenHash) {
        return (root, query, cb) -> cb.equal(root.get("tokenHash"), tokenHash);
    }
}
