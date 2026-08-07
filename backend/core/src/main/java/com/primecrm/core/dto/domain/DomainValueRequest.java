package com.primecrm.core.dto.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record DomainValueRequest(

        @NotBlank(message = "domainTypeCode e obrigatorio")
        @Size(max = 60)
        String domainTypeCode,

        @Size(max = 80)
        String code,

        @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 150)
        String name,

        String description,

        @Size(max = 7)
        String color,

        @Size(max = 60)
        String icon,

        Integer displayOrder,

        Map<String, Object> extra,

        Boolean active
) {
}
