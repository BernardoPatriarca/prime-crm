package com.primecrm.core.security;

import java.util.List;
import java.util.UUID;

public record AuthenticatedUser(
        UUID id,
        String email,
        String login,
        String name,
        List<String> roles,
        List<String> permissions
) {
}
