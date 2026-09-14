package com.primecrm.core.dto.product;

import java.util.UUID;

public record ProductListFilter(
        String search,
        UUID categoryId,
        UUID unitId,
        Boolean service,
        Boolean active
) {
}
