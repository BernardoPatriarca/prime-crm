package com.primecrm.core.dto.pipeline;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PipelineStageRequest(

        @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 150)
        String name,

        Integer displayOrder,

        @DecimalMin(value = "0", message = "defaultProbability deve ser >= 0")
        @DecimalMax(value = "100", message = "defaultProbability deve ser <= 100")
        BigDecimal defaultProbability,

        @Positive(message = "slaDays deve ser positivo")
        Integer slaDays,

        @Size(max = 7)
        String color,

        Boolean requiresLossReason
) {
}
