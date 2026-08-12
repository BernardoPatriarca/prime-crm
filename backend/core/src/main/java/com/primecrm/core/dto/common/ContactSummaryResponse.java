package com.primecrm.core.dto.common;

import java.util.UUID;

public record ContactSummaryResponse(
        UUID id,
        String name,
        String email
) {
}
