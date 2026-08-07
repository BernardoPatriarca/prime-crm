package com.primecrm.core.dto.role;

import com.primecrm.core.dto.permission.PermissionResponse;
import java.util.List;
import java.util.UUID;

public record RoleResponse(
        UUID id,
        String name,
        String description,
        boolean active,
        List<PermissionResponse> permissions
) {
}
