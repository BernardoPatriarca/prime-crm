package com.primecrm.core.dto.commercial;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record LeadRequest(

        @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 200)
        String name,

        @Size(max = 200)
        String companyName,

        @Size(max = 200)
        String contactName,

        @Email(message = "E-mail invalido")
        @Size(max = 180)
        String email,

        @Size(max = 30)
        String phone,

        @Size(max = 30)
        String mobile,

        UUID originId,
        UUID statusId,
        UUID priorityId,

        @Size(max = 150)
        String campaign,

        UUID ownerUserId,
        UUID pipelineId,
        UUID stageId,

        @DecimalMin("0.00")
        @DecimalMax("100.00")
        BigDecimal probability,

        BigDecimal estimatedValue,

        LocalDate expectedCloseDate,

        @Min(0)
        @Max(100)
        Integer qualificationScore,

        String notes,

        List<UUID> tagIds,

        Boolean active
) {
}
