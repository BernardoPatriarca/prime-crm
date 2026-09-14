package com.primecrm.core.dto.proposal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record ProposalItemRequest(

        @NotNull(message = "Produto e obrigatorio")
        UUID productId,

        @Size(max = 300)
        String description,

        @NotNull(message = "Quantidade e obrigatoria")
        @DecimalMin(value = "0.001", message = "Quantidade deve ser maior que zero")
        BigDecimal quantity,

        @DecimalMin(value = "0.00", message = "Preco unitario nao pode ser negativo")
        BigDecimal unitPrice,

        @DecimalMin(value = "0.00", message = "Desconto nao pode ser negativo")
        @DecimalMax(value = "100.00", message = "Desconto nao pode ser maior que 100%")
        BigDecimal discountPercent,

        Integer displayOrder
) {
}
