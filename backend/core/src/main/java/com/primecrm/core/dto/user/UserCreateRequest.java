package com.primecrm.core.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(

        @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 150)
        String name,

        @NotBlank(message = "E-mail e obrigatorio")
        @Email(message = "E-mail invalido")
        @Size(max = 180)
        String email,

        @NotBlank(message = "Login e obrigatorio")
        @Size(max = 80)
        String login,

        @NotBlank(message = "Senha e obrigatoria")
        @Size(min = 8, max = 72, message = "Senha deve ter entre 8 e 72 caracteres")
        String password,

        @Size(max = 30)
        String phone
) {
}
