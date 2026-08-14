package com.primecrm.core.dto.search;

import java.util.UUID;

public record SearchResultResponse(
        SearchResultType type,
        UUID id,
        String code,
        String title,
        String subtitle,
        String link
) {
}
