package com.primecrm.core.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(

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

        @Size(max = 30)
        String phone
) {
}
