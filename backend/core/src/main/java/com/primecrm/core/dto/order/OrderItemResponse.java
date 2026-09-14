package com.primecrm.core.dto.order;

import com.primecrm.core.dto.common.ProductSummaryResponse;
import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
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
