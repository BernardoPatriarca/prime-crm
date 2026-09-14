package com.primecrm.core.dto.product;

import com.primecrm.core.dto.common.DomainValueSummaryResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String code,
        String name,
        String description,
        String sku,
        DomainValueSummaryResponse category,
        DomainValueSummaryResponse unit,
        BigDecimal unitPrice,
        BigDecimal costPrice,
        boolean service,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
