package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.primecrm.core.dto.permission.PermissionResponse;
import com.primecrm.core.mapper.PermissionMapper;
import com.primecrm.infra.entity.auth.Permission;
import com.primecrm.infra.entity.auth.PermissionAction;
import com.primecrm.infra.repository.PermissionRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private PermissionMapper permissionMapper;

    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        permissionService = new PermissionService(permissionRepository, permissionMapper);
    }

    private Permission permission(String module, PermissionAction action) {
        Permission permission = new Permission();
        permission.setId(UUID.randomUUID());
        permission.setModule(module);
        permission.setAction(action);
        permission.setCode(module.toLowerCase() + "." + action.name().toLowerCase());
        return permission;
    }

    @Test
    void findAll_mapsEveryPermissionSortedByModuleAndAction() {
        Permission usersView = permission("USERS", PermissionAction.VIEW);
        Permission rolesCreate = permission("ROLES", PermissionAction.CREATE);

        PermissionResponse usersViewResponse =
                new PermissionResponse(usersView.getId(), "users.view", "USERS", PermissionAction.VIEW, null);
        PermissionResponse rolesCreateResponse =
                new PermissionResponse(rolesCreate.getId(), "roles.create", "ROLES", PermissionAction.CREATE, null);

        when(permissionRepository.findAll()).thenReturn(List.of(usersView, rolesCreate));
        when(permissionMapper.toResponse(usersView)).thenReturn(usersViewResponse);
        when(permissionMapper.toResponse(rolesCreate)).thenReturn(rolesCreateResponse);

        assertThat(permissionService.findAll()).containsExactly(rolesCreateResponse, usersViewResponse);
    }

    @Test
    void findAll_withoutPermissions_returnsEmptyList() {
        when(permissionRepository.findAll()).thenReturn(List.of());

        assertThat(permissionService.findAll()).isEmpty();
    }
}
