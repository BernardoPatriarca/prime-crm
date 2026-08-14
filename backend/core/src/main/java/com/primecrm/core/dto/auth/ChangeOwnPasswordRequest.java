package com.primecrm.core.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeOwnPasswordRequest(

        @NotBlank(message = "Senha atual e obrigatoria")
        String currentPassword,

        @NotBlank(message = "Nova senha e obrigatoria")
        @Size(min = 8, max = 72, message = "Senha deve ter entre 8 e 72 caracteres")
        String newPassword
) {
}
