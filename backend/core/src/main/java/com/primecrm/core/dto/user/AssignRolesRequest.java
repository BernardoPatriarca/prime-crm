package com.primecrm.core.dto.user;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record AssignRolesRequest(

        @NotNull(message = "Lista de roles e obrigatoria (pode ser vazia)")
        List<UUID> roleIds
) {
}
