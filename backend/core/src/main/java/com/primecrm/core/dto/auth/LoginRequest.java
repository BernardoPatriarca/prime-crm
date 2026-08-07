package com.primecrm.core.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "Login ou e-mail e obrigatorio")
        String usernameOrEmail,

        @NotBlank(message = "Senha e obrigatoria")
        String password
) {
}
