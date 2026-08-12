package com.primecrm.core.dto.common;

import java.util.UUID;

public record PipelineSummaryResponse(
        UUID id,
        String name
) {
}
