package com.primecrm.core.dto.finance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReceivableRequest(

        @NotNull(message = "Cliente e obrigatorio")
        UUID customerId,

        UUID orderId,

        UUID contractId,

        @Size(max = 200)
        String description,

        int installmentNumber,

        int totalInstallments,

        @NotNull(message = "Vencimento e obrigatorio")
        LocalDate dueDate,

        @NotNull(message = "Valor e obrigatorio")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        BigDecimal amount,

        UUID paymentMethodId,

        String notes
) {
}
