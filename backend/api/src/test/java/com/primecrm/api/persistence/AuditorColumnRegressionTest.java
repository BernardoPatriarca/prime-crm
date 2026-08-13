package com.primecrm.api.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.primecrm.api.performance.LocalPostgresCondition;
import com.primecrm.core.dto.user.UserCreateRequest;
import com.primecrm.core.security.AuthenticatedUser;
import com.primecrm.core.service.UserService;
import com.primecrm.infra.entity.auth.User;
import com.primecrm.infra.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
@Transactional
@EnabledIf(value = "com.primecrm.api.performance.LocalPostgresCondition#isReachable",
        disabledReason = "Postgres local nao esta acessivel neste ambiente")
class AuditorColumnRegressionTest {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_whileAuthenticated_storesTheLoginInCreatedBy() {
        authenticateAsAdministrator();
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        UUID id = userService.create(new UserCreateRequest("Auditor " + suffix,
                "auditor." + suffix + "@primecrm.local", "auditor." + suffix, "Admin@123", null)).id();
        entityManager.flush();
        entityManager.clear();

        User created = userRepository.findById(id).orElseThrow();
        assertThat(created.getCreatedBy()).isEqualTo("admin");
    }

    private void authenticateAsAdministrator() {
        List<String> permissions = IntStream.range(0, 60).mapToObj("MODULO_%d_VIEW"::formatted).toList();
        AuthenticatedUser principal = new AuthenticatedUser(UUID.randomUUID(), "admin@primecrm.local", "admin",
                "Administrador", List.of("Administrador"), permissions);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
