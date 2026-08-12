package com.primecrm.core.dto.commercial;

import com.primecrm.core.dto.common.CustomerSummaryResponse;
import com.primecrm.core.dto.common.UserSummaryResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record OpportunityCardResponse(
        UUID id,
        String code,
        String title,
        BigDecimal amount,
        BigDecimal probability,
        LocalDate expectedCloseDate,
        Instant openedAt,
        CustomerSummaryResponse customer,
        UserSummaryResponse owner
) {
}
