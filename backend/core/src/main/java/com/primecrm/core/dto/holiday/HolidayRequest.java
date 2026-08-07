package com.primecrm.core.dto.holiday;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record HolidayRequest(

        @NotNull(message = "holidayDate e obrigatorio")
        LocalDate holidayDate,

        @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 150)
        String name,

        Boolean national,

        Boolean active
) {
}
