package com.primecrm.core.dto.commercial;

import com.primecrm.core.dto.common.CustomerSummaryResponse;
import com.primecrm.core.dto.common.DomainValueSummaryResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ContactResponse(
        UUID id,
        CustomerSummaryResponse customer,
        String name,
        String positionTitle,
        DomainValueSummaryResponse department,
        String email,
        String phone,
        String mobile,
        LocalDate birthDate,
        String linkedin,
        boolean primaryContact,
        boolean decisionMaker,
        String notes,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
