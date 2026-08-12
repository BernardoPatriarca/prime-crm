package com.primecrm.core.dto.commercial;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;

@Builder
public record OpportunityRequest(

        @NotBlank(message = "Titulo e obrigatorio")
        @Size(max = 200)
        String title,

        @NotNull(message = "Cliente e obrigatorio")
        UUID customerId,

        UUID contactId,

        @NotNull(message = "Funil e obrigatorio")
        UUID pipelineId,

        UUID stageId,

        BigDecimal amount,

        @DecimalMin("0.00")
        @DecimalMax("100.00")
        BigDecimal probability,

        UUID ownerUserId,
        UUID teamId,

        LocalDate expectedCloseDate,

        @Size(max = 200)
        String competitor,

        UUID sourceLeadId,

        String notes
) {
}
