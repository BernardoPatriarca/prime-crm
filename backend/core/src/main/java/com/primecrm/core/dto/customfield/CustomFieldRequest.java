package com.primecrm.core.dto.customfield;

import com.primecrm.infra.entity.config.FieldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record CustomFieldRequest(

        @NotBlank(message = "targetEntity e obrigatorio")
        @Size(max = 60)
        String targetEntity,

        @NotBlank(message = "fieldKey e obrigatorio")
        @Size(max = 100)
        String fieldKey,

        @NotBlank(message = "Label e obrigatorio")
        @Size(max = 150)
        String label,

        @NotNull(message = "fieldType e obrigatorio")
        FieldType fieldType,

        Map<String, Object> options,

        Boolean required,

        Integer displayOrder,

        Boolean active
) {
}
