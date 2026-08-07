package com.primecrm.core.dto.pipeline;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PipelineRequest(

        @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 150)
        String name,

        @Size(max = 100)
        String businessType,

        Boolean active
) {
}
