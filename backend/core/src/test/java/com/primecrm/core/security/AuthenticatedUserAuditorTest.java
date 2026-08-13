package com.primecrm.core.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.primecrm.infra.config.JpaAuditingConfig;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AuthenticatedUserAuditorTest {

    private static final int CREATED_BY_COLUMN_LENGTH = 120;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticationName_isTheLogin_notThePrincipalToString() {
        authenticate(userWithManyPermissions());

        String name = SecurityContextHolder.getContext().getAuthentication().getName();

        assertThat(name).isEqualTo("admin");
    }

    @Test
    void auditor_ofAUserWithManyPermissions_fitsTheCreatedByColumn() {
        authenticate(userWithManyPermissions());

        String auditor = new JpaAuditingConfig().auditorProvider().getCurrentAuditor().orElseThrow();

        assertThat(auditor).isEqualTo("admin");
        assertThat(auditor.length()).isLessThanOrEqualTo(CREATED_BY_COLUMN_LENGTH);
    }

    @Test
    void auditor_withoutAuthentication_fallsBackToSystem() {
        assertThat(new JpaAuditingConfig().auditorProvider().getCurrentAuditor()).contains("system");
    }

    private void authenticate(AuthenticatedUser user) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private AuthenticatedUser userWithManyPermissions() {
        List<String> permissions = IntStream.range(0, 60).mapToObj("MODULO_%d_VIEW"::formatted).toList();
        return new AuthenticatedUser(UUID.randomUUID(), "admin@primecrm.local", "admin", "Administrador",
                List.of("Administrador"), permissions);
    }
}
