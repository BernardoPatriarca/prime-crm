package com.primecrm.core.service.support;

import com.primecrm.infra.entity.auth.Permission;
import com.primecrm.infra.entity.auth.Role;
import com.primecrm.infra.entity.auth.RolePermission;
import com.primecrm.infra.entity.auth.UserRole;
import com.primecrm.infra.repository.RolePermissionRepository;
import com.primecrm.infra.repository.UserRoleRepository;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAuthorityResolver {

    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public List<Role> resolveActiveRoles(UUID userId) {
        return resolveActiveRolesByUser(List.of(userId)).getOrDefault(userId, List.of());
    }

    public Map<UUID, List<Role>> resolveActiveRolesByUser(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRoleRepository.findByUser_IdIn(userIds).stream()
                .filter(userRole -> isUsable(userRole.getRole()))
                .collect(Collectors.groupingBy(userRole -> userRole.getUser().getId(),
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(
                                Collectors.mapping(UserRole::getRole, Collectors.toList()),
                                roles -> roles.stream().distinct().toList())));
    }

    public List<String> resolveRoleNames(List<Role> activeRoles) {
        return activeRoles.stream().map(Role::getName).distinct().toList();
    }

    public List<String> resolvePermissionCodes(List<Role> activeRoles) {
        if (activeRoles.isEmpty()) {
            return List.of();
        }
        List<UUID> roleIds = activeRoles.stream().map(Role::getId).toList();
        return rolePermissionRepository.findByRole_IdIn(roleIds).stream()
                .map(RolePermission::getPermission)
                .map(Permission::getCode)
                .distinct()
                .toList();
    }

    private boolean isUsable(Role role) {
        return role.isActive() && !role.isDeleted();
    }
}
