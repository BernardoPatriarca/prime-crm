package com.primecrm.core.dto.contract;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ContractRequest(

        @NotNull(message = "Cliente e obrigatorio")
        UUID customerId,

        UUID opportunityId,

        UUID ownerUserId,

        UUID billingCycleId,

        LocalDate startDate,

        LocalDate endDate,

        boolean autoRenew,

        @DecimalMin(value = "0.00", message = "Valor recorrente nao pode ser negativo")
        BigDecimal recurringAmount,

        String notes
) {
}
