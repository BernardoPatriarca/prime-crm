package com.primecrm.core.dto.common;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReorderItem(

        @NotNull(message = "Id e obrigatorio")
        UUID id,

        @NotNull(message = "displayOrder e obrigatorio")
        Integer displayOrder
) {
}
