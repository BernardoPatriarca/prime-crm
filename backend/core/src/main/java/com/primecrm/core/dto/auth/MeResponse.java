package com.primecrm.core.dto.auth;

import com.primecrm.infra.entity.auth.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MeResponse(
        UUID id,
        String name,
        String email,
        String login,
        UserStatus status,
        Instant lastLoginAt,
        List<String> roles,
        List<String> permissions
) {
}
