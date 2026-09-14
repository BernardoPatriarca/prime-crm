package com.primecrm.core.dto.proposal;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record ProposalRequest(

        @NotNull(message = "Cliente e obrigatorio")
        UUID customerId,

        UUID contactId,

        UUID opportunityId,

        UUID ownerUserId,

        LocalDate issueDate,

        LocalDate validUntil,

        String notes
) {
}
