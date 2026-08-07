package com.primecrm.core.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RoleRequest(

        @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 100)
        String name,

        @Size(max = 255)
        String description,

        @NotNull(message = "Status ativo/inativo e obrigatorio")
        Boolean active
) {
}
