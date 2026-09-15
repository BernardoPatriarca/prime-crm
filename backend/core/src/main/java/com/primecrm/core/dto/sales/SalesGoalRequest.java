package com.primecrm.core.dto.sales;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SalesGoalRequest(

        @NotNull(message = "Vendedor e obrigatorio")
        UUID ownerUserId,

        @NotNull(message = "Mes de referencia e obrigatorio")
        LocalDate referenceMonth,

        @NotNull(message = "Valor da meta e obrigatorio")
        @DecimalMin(value = "0.01", message = "Valor da meta deve ser maior que zero")
        BigDecimal targetAmount,

        String notes
) {
}
