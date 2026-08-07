package com.primecrm.core.dto.role;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record AssignPermissionsRequest(

        @NotNull(message = "Lista de permissoes e obrigatoria (pode ser vazia)")
        List<UUID> permissionIds
) {
}
