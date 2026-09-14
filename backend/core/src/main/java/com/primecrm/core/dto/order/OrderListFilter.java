package com.primecrm.core.dto.order;

import com.primecrm.infra.entity.order.OrderStatus;
import java.util.UUID;

public record OrderListFilter(
        String search,
        OrderStatus status,
        UUID customerId,
        UUID opportunityId,
        UUID ownerUserId
) {
}
