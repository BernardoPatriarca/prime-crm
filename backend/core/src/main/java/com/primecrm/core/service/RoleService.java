package com.primecrm.core.service;

import com.primecrm.core.audit.AuditChanges;
import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.permission.PermissionResponse;
import com.primecrm.core.dto.role.AssignPermissionsRequest;
import com.primecrm.core.dto.role.RoleRequest;
import com.primecrm.core.dto.role.RoleResponse;
import com.primecrm.core.mapper.PermissionMapper;
import com.primecrm.core.mapper.RoleMapper;
import com.primecrm.core.specification.RolePermissionSpecifications;
import com.primecrm.core.specification.RoleSpecifications;
import com.primecrm.core.specification.SpecificationUtils;
import com.primecrm.infra.entity.audit.AuditAction;
import com.primecrm.infra.entity.auth.Permission;
import com.primecrm.infra.entity.auth.Role;
import com.primecrm.infra.entity.auth.RolePermission;
import com.primecrm.infra.repository.PermissionRepository;
import com.primecrm.infra.repository.RolePermissionRepository;
import com.primecrm.infra.repository.RoleRepository;
import com.primecrm.shared.exception.ConflictException;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoleService {

    private static final String AUDIT_ENTITY = "Role";

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<RoleResponse> list(String search, Boolean active, Pageable pageable) {
        var spec = SpecificationUtils.<Role>and(
                RoleSpecifications.notDeleted(),
                RoleSpecifications.textSearch(search),
                RoleSpecifications.hasActive(active)
        );
        Page<Role> page = roleRepository.findAll(spec, pageable);
        Map<UUID, List<PermissionResponse>> permissionsByRole = loadPermissionsByRole(page.getContent());
        return page.map(role -> roleMapper.toResponse(role,
                permissionsByRole.getOrDefault(role.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public RoleResponse findById(UUID id) {
        return toResponse(getActiveRoleOrThrow(id));
    }

    @Transactional
    public RoleResponse create(RoleRequest request) {
        ensureNameAvailable(request.name(), null);
        Role role = roleMapper.toEntity(request);
        role = roleRepository.save(role);
        auditService.recordCreate(role);
        return toResponse(role);
    }

    @Transactional
    public RoleResponse update(UUID id, RoleRequest request) {
        Role role = getActiveRoleOrThrow(id);
        ensureNameAvailable(request.name(), id);
        Map<String, Object> previousState = auditService.snapshot(role);
        roleMapper.updateEntity(role, request);
        role = roleRepository.save(role);
        auditService.recordUpdate(role, previousState);
        return toResponse(role);
    }

    @Transactional
    public void delete(UUID id) {
        Role role = getActiveRoleOrThrow(id);
        role.setDeletedAt(Instant.now());
        roleRepository.save(role);
        auditService.recordDelete(role);
    }

    @Transactional
    public RoleResponse assignPermissions(UUID id, AssignPermissionsRequest request) {
        Role role = getActiveRoleOrThrow(id);

        List<Permission> permissions = permissionRepository.findAllById(request.permissionIds());
        if (permissions.size() != request.permissionIds().size()) {
            throw new ResourceNotFoundException("Uma ou mais permissoes informadas nao foram encontradas");
        }

        List<RolePermission> current =
                rolePermissionRepository.findAll(RolePermissionSpecifications.byRoleId(id));
        List<String> previousCodes = current.stream()
                .map(rolePermission -> rolePermission.getPermission().getCode())
                .sorted()
                .toList();
        rolePermissionRepository.deleteAll(current);

        List<RolePermission> updated = permissions.stream().map(permission -> {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRole(role);
            rolePermission.setPermission(permission);
            return rolePermission;
        }).toList();
        rolePermissionRepository.saveAll(updated);

        List<String> currentCodes = permissions.stream().map(Permission::getCode).sorted().toList();
        auditService.recordChange(AuditAction.UPDATE, AUDIT_ENTITY, id,
                Map.of("permissions", AuditChanges.of(previousCodes, currentCodes)));

        return toResponse(role);
    }

    private Role getActiveRoleOrThrow(UUID id) {
        return roleRepository.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Role", id));
    }

    private void ensureNameAvailable(String name, UUID excludingId) {
        roleRepository.findAll(RoleSpecifications.notDeleted()).stream()
                .filter(r -> r.getName().equalsIgnoreCase(name))
                .filter(r -> excludingId == null || !r.getId().equals(excludingId))
                .findFirst()
                .ifPresent(r -> {
                    throw new ConflictException("Ja existe um perfil cadastrado com este nome");
                });
    }

    private RoleResponse toResponse(Role role) {
        return roleMapper.toResponse(role,
                loadPermissionsByRole(List.of(role)).getOrDefault(role.getId(), List.of()));
    }

    private Map<UUID, List<PermissionResponse>> loadPermissionsByRole(List<Role> roles) {
        if (roles.isEmpty()) {
            return Map.of();
        }
        List<UUID> roleIds = roles.stream().map(Role::getId).toList();
        return rolePermissionRepository.findByRole_IdIn(roleIds).stream()
                .collect(Collectors.groupingBy(rolePermission -> rolePermission.getRole().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(
                                rolePermission -> permissionMapper.toResponse(rolePermission.getPermission()),
                                Collectors.toList())));
    }
}
