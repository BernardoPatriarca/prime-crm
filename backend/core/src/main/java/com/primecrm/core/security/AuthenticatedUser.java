package com.primecrm.core.security;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.AuthenticatedPrincipal;

public record AuthenticatedUser(
        UUID id,
        String email,
        String login,
        String name,
        List<String> roles,
        List<String> permissions
) implements AuthenticatedPrincipal {

    @Override
    public String getName() {
        return login;
    }
}
