package com.primecrm.core.dto.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ReorderRequest(

        @NotEmpty(message = "Lista de itens a reordenar e obrigatoria")
        @Valid
        List<ReorderItem> items
) {
}
