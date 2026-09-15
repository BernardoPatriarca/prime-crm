package com.primecrm.core.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class JwtTokenProviderTest {

    private static final String INSECURE_DEFAULT_SECRET =
            "prime-crm-dev-secret-key-change-me-please-0123456789abcdef";

    @Test
    void init_withProductionProfileAndTheDefaultSecret_throwsIllegalState() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(INSECURE_DEFAULT_SECRET);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        JwtTokenProvider provider = new JwtTokenProvider(properties, environment);

        assertThatThrownBy(provider::init).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void init_withProductionProfileAndACustomSecret_succeeds() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("a-real-production-secret-with-enough-entropy-0123456789");
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        JwtTokenProvider provider = new JwtTokenProvider(properties, environment);

        provider.init();
        UUID userId = UUID.randomUUID();
        String token = provider.generateAccessToken(userId, "ana@primecrm.com", "ana", "Ana",
                List.of("Administrador"), List.of("CLIENTES_VIEW"));

        AuthenticatedUser authenticatedUser = provider.parseAuthenticatedUser(token);
        assertThat(authenticatedUser.id()).isEqualTo(userId);
    }

    @Test
    void init_withDevProfileAndTheDefaultSecret_succeeds() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(INSECURE_DEFAULT_SECRET);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        JwtTokenProvider provider = new JwtTokenProvider(properties, environment);

        provider.init();
    }
}
