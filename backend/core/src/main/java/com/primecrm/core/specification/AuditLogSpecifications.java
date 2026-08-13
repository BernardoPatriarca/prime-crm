package com.primecrm.core.specification;

import com.primecrm.infra.entity.audit.AuditAction;
import com.primecrm.infra.entity.audit.AuditLog;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class AuditLogSpecifications {

    private AuditLogSpecifications() {
    }

    public static Specification<AuditLog> hasEntityName(String entityName) {
        if (!StringUtils.hasText(entityName)) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("entityName"), entityName);
    }

    public static Specification<AuditLog> hasEntityId(UUID entityId) {
        if (entityId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("entityId"), entityId);
    }

    public static Specification<AuditLog> hasAction(AuditAction action) {
        if (action == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("action"), action);
    }

    public static Specification<AuditLog> hasUser(UUID userId) {
        if (userId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<AuditLog> from(Instant from) {
        if (from == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<AuditLog> to(Instant to) {
        if (to == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    public static Specification<AuditLog> textSearch(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("entityName")), pattern),
                cb.like(cb.lower(root.get("userEmail")), pattern),
                cb.like(cb.lower(root.get("ipAddress")), pattern));
    }
}
