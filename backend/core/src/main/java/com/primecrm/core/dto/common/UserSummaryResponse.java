package com.primecrm.core.dto.common;

import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        String name,
        String email
) {
}
