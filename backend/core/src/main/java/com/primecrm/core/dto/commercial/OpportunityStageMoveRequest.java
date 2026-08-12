package com.primecrm.core.dto.commercial;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record OpportunityStageMoveRequest(

        @NotNull(message = "Etapa de destino e obrigatoria")
        UUID stageId,

        UUID lossReasonId,

        UUID winReasonId,

        String note
) {
}
