package com.primecrm.core.dto.user;

import com.primecrm.infra.entity.auth.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        String login,
        String phone,
        UserStatus status,
        Instant lastLoginAt,
        Instant createdAt,
        List<RoleSummaryResponse> roles
) {
}
