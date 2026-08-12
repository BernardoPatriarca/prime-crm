package com.primecrm.core.dto.commercial;

import com.primecrm.infra.entity.commercial.PersonType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CustomerRequest(

        @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 200)
        String name,

        @Size(max = 200)
        String tradeName,

        @NotNull(message = "Tipo de pessoa e obrigatorio")
        PersonType personType,

        @Size(max = 20)
        String document,

        @Size(max = 30)
        String stateRegistration,

        @Size(max = 30)
        String municipalRegistration,

        UUID clientTypeId,
        UUID segmentId,
        UUID activityBranchId,
        UUID categoryId,
        UUID originId,
        UUID statusId,
        UUID ownerUserId,
        UUID teamId,

        @Size(max = 30)
        String phone,

        @Size(max = 30)
        String mobile,

        @Email(message = "E-mail invalido")
        @Size(max = 180)
        String email,

        @Email(message = "E-mail financeiro invalido")
        @Size(max = 180)
        String financialEmail,

        @Size(max = 255)
        String website,

        @Size(max = 120)
        String instagram,

        @Size(max = 255)
        String linkedin,

        @Size(max = 20)
        String zipCode,

        @Size(max = 200)
        String street,

        @Size(max = 20)
        String number,

        @Size(max = 120)
        String complement,

        @Size(max = 120)
        String district,

        @Size(max = 120)
        String city,

        @Size(max = 2)
        String state,

        @Size(max = 60)
        String country,

        BigDecimal latitude,
        BigDecimal longitude,
        LocalDate birthDate,
        Instant lastContactAt,
        Instant nextContactAt,
        BigDecimal creditLimit,

        @Size(max = 120)
        String paymentTerms,

        @Min(0)
        @Max(100)
        Integer healthScore,

        String notes,

        UUID parentCustomerId,

        List<UUID> tagIds,

        Boolean active
) {
}
