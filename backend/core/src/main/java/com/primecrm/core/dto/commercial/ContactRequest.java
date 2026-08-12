package com.primecrm.core.dto.commercial;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record ContactRequest(

        @NotNull(message = "Cliente e obrigatorio")
        UUID customerId,

        @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 200)
        String name,

        @Size(max = 120)
        String positionTitle,

        UUID departmentId,

        @Email(message = "E-mail invalido")
        @Size(max = 180)
        String email,

        @Size(max = 30)
        String phone,

        @Size(max = 30)
        String mobile,

        LocalDate birthDate,

        @Size(max = 255)
        String linkedin,

        Boolean primaryContact,

        Boolean decisionMaker,

        String notes,

        Boolean active
) {
}
