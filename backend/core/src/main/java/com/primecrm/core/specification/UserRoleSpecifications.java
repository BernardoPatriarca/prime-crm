package com.primecrm.core.specification;

import com.primecrm.infra.entity.auth.UserRole;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class UserRoleSpecifications {

    private UserRoleSpecifications() {
    }

    public static Specification<UserRole> byUserId(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }
}
