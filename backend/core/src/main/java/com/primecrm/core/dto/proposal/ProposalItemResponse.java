package com.primecrm.core.dto.proposal;

import com.primecrm.core.dto.common.ProductSummaryResponse;
import java.math.BigDecimal;
import java.util.UUID;

public record ProposalItemResponse(
        UUID id,
        ProductSummaryResponse product,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal discountPercent,
        BigDecimal total,
        int displayOrder
) {
}
