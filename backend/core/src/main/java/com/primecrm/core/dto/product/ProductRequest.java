package com.primecrm.core.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductRequest(

        @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 200)
        String name,

        String description,

        @Size(max = 60)
        String sku,

        UUID categoryId,

        UUID unitId,

        @NotNull(message = "Preco de venda e obrigatorio")
        @DecimalMin(value = "0.00", message = "Preco de venda nao pode ser negativo")
        BigDecimal unitPrice,

        @DecimalMin(value = "0.00", message = "Preco de custo nao pode ser negativo")
        BigDecimal costPrice,

        boolean service,

        boolean active
) {
}
