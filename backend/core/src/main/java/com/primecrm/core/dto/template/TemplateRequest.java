package com.primecrm.core.dto.template;

import com.primecrm.infra.entity.config.TemplateType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TemplateRequest(

        @NotNull(message = "type e obrigatorio")
        TemplateType type,

        @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 150)
        String name,

        @Size(max = 255)
        String subject,

        @NotBlank(message = "Conteudo e obrigatorio")
        String content,

        Boolean active
) {
}
