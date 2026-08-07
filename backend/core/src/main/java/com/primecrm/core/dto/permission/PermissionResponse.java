package com.primecrm.core.dto.permission;

import com.primecrm.infra.entity.auth.PermissionAction;
import java.util.UUID;

public record PermissionResponse(
        UUID id,
        String code,
        String module,
        PermissionAction action,
        String description
) {
}
