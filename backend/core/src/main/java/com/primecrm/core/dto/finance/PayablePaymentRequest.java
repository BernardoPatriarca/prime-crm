package com.primecrm.core.dto.finance;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PayablePaymentRequest(

        @DecimalMin(value = "0.01", message = "Valor pago deve ser maior que zero")
        BigDecimal amount,

        Instant paidAt,

        UUID paymentMethodId
) {
}
