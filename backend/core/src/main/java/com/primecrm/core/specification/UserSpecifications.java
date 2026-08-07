package com.primecrm.core.specification;

import com.primecrm.infra.entity.auth.User;
import com.primecrm.infra.entity.auth.UserStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<User> hasStatus(UserStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<User> textSearch(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            Predicate byName = cb.like(cb.lower(root.get("name")), pattern);
            Predicate byEmail = cb.like(cb.lower(root.get("email")), pattern);
            Predicate byLogin = cb.like(cb.lower(root.get("login")), pattern);
            return cb.or(byName, byEmail, byLogin);
        };
    }

    public static Specification<User> byLoginOrEmail(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        return (root, query, cb) -> cb.or(
                cb.equal(cb.lower(root.get("login")), normalized),
                cb.equal(cb.lower(root.get("email")), normalized)
        );
    }
}
