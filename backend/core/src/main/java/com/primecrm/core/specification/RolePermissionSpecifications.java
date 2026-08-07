package com.primecrm.core.specification;

import com.primecrm.infra.entity.auth.RolePermission;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class RolePermissionSpecifications {

    private RolePermissionSpecifications() {
    }

    public static Specification<RolePermission> byRoleId(UUID roleId) {
        return (root, query, cb) -> cb.equal(root.get("role").get("id"), roleId);
    }

    public static Specification<RolePermission> byRoleIds(Collection<UUID> roleIds) {
        return (root, query, cb) -> root.get("role").get("id").in(roleIds);
    }
}
