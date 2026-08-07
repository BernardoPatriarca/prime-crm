package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.audit.AuditChanges;
import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.role.AssignPermissionsRequest;
import com.primecrm.core.dto.role.RoleRequest;
import com.primecrm.core.dto.role.RoleResponse;
import com.primecrm.core.mapper.PermissionMapper;
import com.primecrm.core.mapper.RoleMapper;
import com.primecrm.infra.entity.audit.AuditAction;
import com.primecrm.infra.entity.auth.Permission;
import com.primecrm.infra.entity.auth.PermissionAction;
import com.primecrm.infra.entity.auth.Role;
import com.primecrm.infra.entity.auth.RolePermission;
import com.primecrm.infra.repository.PermissionRepository;
import com.primecrm.infra.repository.RolePermissionRepository;
import com.primecrm.infra.repository.RoleRepository;
import com.primecrm.shared.exception.ConflictException;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RolePermissionRepository rolePermissionRepository;
    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private PermissionMapper permissionMapper;
    @Mock
    private AuditService auditService;

    private RoleService roleService;

    @BeforeEach
    void setUp() {
        roleService = new RoleService(roleRepository, rolePermissionRepository, permissionRepository, roleMapper,
                permissionMapper, auditService);
    }

    private Permission permission(String code) {
        Permission permission = new Permission();
        permission.setId(UUID.randomUUID());
        permission.setCode(code);
        permission.setModule("USERS");
        permission.setAction(PermissionAction.VIEW);
        return permission;
    }

    private Role role(UUID id, String name) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        return role;
    }

    @Test
    void create_withDuplicateNameIgnoringCase_throwsConflict() {
        RoleRequest request = new RoleRequest("administrador", "Perfil admin", null);
        when(roleRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(role(UUID.randomUUID(), "Administrador")));

        assertThatThrownBy(() -> roleService.create(request))
                .isInstanceOf(ConflictException.class);

        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    void create_withAvailableName_savesRoleAndAudits() {
        RoleRequest request = new RoleRequest("Vendedor", "Perfil de vendas", null);

        Role mapped = new Role();
        Role saved = role(UUID.randomUUID(), "Vendedor");
        RoleResponse expected = new RoleResponse(saved.getId(), "Vendedor", "Perfil de vendas", true, List.of());

        when(roleRepository.findAll(any(Specification.class))).thenReturn(List.of());
        when(roleMapper.toEntity(request)).thenReturn(mapped);
        when(roleRepository.save(mapped)).thenReturn(saved);
        when(rolePermissionRepository.findByRole_IdIn(anyCollection())).thenReturn(List.of());
        when(roleMapper.toResponse(eq(saved), anyList())).thenReturn(expected);

        assertThat(roleService.create(request)).isEqualTo(expected);
        verify(auditService).recordCreate(saved);
    }

    @Test
    void update_keepingOwnName_doesNotThrowConflict() {
        UUID id = UUID.randomUUID();
        RoleRequest request = new RoleRequest("Vendedor", "Atualizado", null);
        Role existing = role(id, "Vendedor");

        when(roleRepository.findById(id)).thenReturn(Optional.of(existing));
        when(roleRepository.findAll(any(Specification.class))).thenReturn(List.of(existing));
        when(roleRepository.save(existing)).thenReturn(existing);
        when(rolePermissionRepository.findByRole_IdIn(anyCollection())).thenReturn(List.of());

        roleService.update(id, request);

        verify(roleRepository).save(existing);
        verify(auditService).recordUpdate(eq(existing), any());
    }

    @Test
    void update_withNameOwnedByAnotherRole_throwsConflict() {
        UUID id = UUID.randomUUID();
        RoleRequest request = new RoleRequest("Administrador", "Atualizado", null);

        when(roleRepository.findById(id)).thenReturn(Optional.of(role(id, "Vendedor")));
        when(roleRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(role(UUID.randomUUID(), "Administrador")));

        assertThatThrownBy(() -> roleService.update(id, request))
                .isInstanceOf(ConflictException.class);

        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    void delete_existingRole_softDeletesAndAudits() {
        UUID id = UUID.randomUUID();
        Role existing = role(id, "Vendedor");

        when(roleRepository.findById(id)).thenReturn(Optional.of(existing));
        when(roleRepository.save(existing)).thenReturn(existing);

        roleService.delete(id);

        assertThat(existing.getDeletedAt()).isNotNull();
        verify(auditService).recordDelete(existing);
    }

    @Test
    void assignPermissions_withUnknownPermission_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        AssignPermissionsRequest request =
                new AssignPermissionsRequest(List.of(UUID.randomUUID(), UUID.randomUUID()));

        when(roleRepository.findById(id)).thenReturn(Optional.of(role(id, "Vendedor")));
        when(permissionRepository.findAllById(request.permissionIds())).thenReturn(List.of(permission("users.view")));

        assertThatThrownBy(() -> roleService.assignPermissions(id, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(rolePermissionRepository, never()).saveAll(anyList());
    }

    @Test
    void assignPermissions_replacesWholePermissionSetAndAuditsOldAndNewCodes() {
        UUID id = UUID.randomUUID();
        Role role = role(id, "Vendedor");

        Permission granted = permission("users.edit");
        AssignPermissionsRequest request = new AssignPermissionsRequest(List.of(granted.getId()));

        RolePermission previous = new RolePermission();
        previous.setId(UUID.randomUUID());
        previous.setRole(role);
        previous.setPermission(permission("users.view"));

        when(roleRepository.findById(id)).thenReturn(Optional.of(role));
        when(permissionRepository.findAllById(request.permissionIds())).thenReturn(List.of(granted));
        when(rolePermissionRepository.findAll(any(Specification.class))).thenReturn(List.of(previous));

        roleService.assignPermissions(id, request);

        verify(rolePermissionRepository).deleteAll(List.of(previous));

        ArgumentCaptor<List<RolePermission>> savedCaptor = ArgumentCaptor.forClass(List.class);
        verify(rolePermissionRepository).saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue()).hasSize(1);
        assertThat(savedCaptor.getValue().get(0).getPermission()).isEqualTo(granted);

        ArgumentCaptor<Map<String, Object>> changesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService).recordChange(eq(AuditAction.UPDATE), eq("Role"), eq(id), changesCaptor.capture());
        assertThat(changesCaptor.getValue().get("permissions")).isEqualTo(
                AuditChanges.of(List.of("users.view"), List.of("users.edit")));
    }
}
