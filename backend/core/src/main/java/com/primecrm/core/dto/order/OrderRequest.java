package com.primecrm.core.dto.order;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record OrderRequest(

        @NotNull(message = "Cliente e obrigatorio")
        UUID customerId,

        UUID opportunityId,

        UUID ownerUserId,

        LocalDate orderDate,

        LocalDate deliveryDate,

        String notes
) {
}
