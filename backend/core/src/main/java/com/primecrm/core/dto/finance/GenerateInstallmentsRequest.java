package com.primecrm.core.dto.finance;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record GenerateInstallmentsRequest(

        @NotNull(message = "Numero de parcelas e obrigatorio")
        @Min(value = 1, message = "Numero de parcelas deve ser pelo menos 1")
        @Max(value = 60, message = "Numero de parcelas nao pode ser maior que 60")
        Integer installments,

        @NotNull(message = "Vencimento da primeira parcela e obrigatorio")
        LocalDate firstDueDate
) {
}
