package com.primecrm.core.specification;

import com.primecrm.infra.entity.auth.RefreshToken;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class RefreshTokenSpecifications {

    private RefreshTokenSpecifications() {
    }

    public static Specification<RefreshToken> byTokenHash(String tokenHash) {
        return (root, query, cb) -> cb.equal(root.get("tokenHash"), tokenHash);
    }

    public static Specification<RefreshToken> activeByUserId(UUID userId) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("user").get("id"), userId),
                cb.isFalse(root.get("revoked")));
    }
}
