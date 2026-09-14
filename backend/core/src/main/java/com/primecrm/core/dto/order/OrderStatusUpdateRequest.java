package com.primecrm.core.dto.order;

import com.primecrm.infra.entity.order.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(

        @NotNull(message = "Status e obrigatorio")
        OrderStatus status
) {
}
