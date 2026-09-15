package com.primecrm.core.dto.finance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PayableRequest(

        @NotNull(message = "Fornecedor e obrigatorio")
        UUID supplierId,

        UUID categoryId,

        @NotBlank(message = "Descricao e obrigatoria")
        @Size(max = 200)
        String description,

        @NotNull(message = "Vencimento e obrigatorio")
        LocalDate dueDate,

        @NotNull(message = "Valor e obrigatorio")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        BigDecimal amount,

        UUID paymentMethodId,

        String notes
) {
}
