package com.primecrm.core.dto.search;

import java.util.List;

public record GlobalSearchResponse(
        String query,
        int total,
        List<SearchResultResponse> results
) {
}
