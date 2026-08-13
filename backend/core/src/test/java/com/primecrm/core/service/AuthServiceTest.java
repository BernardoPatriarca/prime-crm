package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.dto.auth.LoginRequest;
import com.primecrm.core.dto.auth.LoginResponse;
import com.primecrm.core.dto.auth.RefreshTokenRequest;
import com.primecrm.core.security.JwtTokenProvider;
import com.primecrm.core.service.support.UserAuthorityResolver;
import com.primecrm.infra.entity.auth.Role;
import com.primecrm.infra.entity.auth.User;
import com.primecrm.infra.entity.auth.UserStatus;
import com.primecrm.infra.entity.auth.RefreshToken;
import com.primecrm.infra.repository.RefreshTokenRepository;
import com.primecrm.infra.repository.UserRepository;
import com.primecrm.shared.exception.UnauthorizedException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import com.primecrm.core.audit.AuditService;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private UserAuthorityResolver authorityResolver;
    @Mock
    private AuditService auditService;

    private AuthService authService;

    private User adminUser;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder,
                jwtTokenProvider, authorityResolver, auditService);

        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setName("Administrador");
        adminUser.setEmail("admin@primecrm.local");
        adminUser.setLogin("admin");
        adminUser.setPasswordHash("$2a$10$hashvalue");
        adminUser.setStatus(UserStatus.ACTIVE);

        adminRole = new Role();
        adminRole.setId(UUID.randomUUID());
        adminRole.setName("Administrador");
        adminRole.setActive(true);
    }

    @Test
    void login_withCorrectPassword_returnsTokenPair() {
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("Admin@123", adminUser.getPasswordHash())).thenReturn(true);
        when(authorityResolver.resolveActiveRoles(adminUser.getId())).thenReturn(List.of(adminRole));
        when(authorityResolver.resolveRoleNames(List.of(adminRole))).thenReturn(List.of("Administrador"));
        when(authorityResolver.resolvePermissionCodes(List.of(adminRole))).thenReturn(List.of("USUARIOS_VIEW"));
        when(jwtTokenProvider.generateAccessToken(any(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn("fake-access-token");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(jwtTokenProvider.getRefreshTokenExpirationDays()).thenReturn(7L);
        when(userRepository.save(any(User.class))).thenReturn(adminUser);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        LoginResponse response = authService.login(new LoginRequest("admin", "Admin@123"));

        assertThat(response.accessToken()).isEqualTo("fake-access-token");
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(900L);
        assertThat(response.user().login()).isEqualTo("admin");
        assertThat(response.user().roles()).containsExactly("Administrador");
        assertThat(response.user().permissions()).containsExactly("USUARIOS_VIEW");

        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(userRepository).save(adminUser);
    }

    @Test
    void login_withWrongPassword_throwsUnauthorized() {
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("wrong-password", adminUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "wrong-password")))
                .isInstanceOf(UnauthorizedException.class);

        verify(userRepository, org.mockito.Mockito.never()).save(any(User.class));
    }

    @Test
    void login_withUnknownUser_throwsUnauthorized() {
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "whatever")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_withInactiveUser_throwsUnauthorized() {
        adminUser.setStatus(UserStatus.BLOCKED);
        when(userRepository.findOne(any(Specification.class))).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("Admin@123", adminUser.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "Admin@123")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refresh_withRevokedToken_throwsUnauthorized() {
        RefreshToken stored = new RefreshToken();
        stored.setUser(adminUser);
        stored.setRevoked(true);
        stored.setExpiresAt(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findOne(any(Specification.class))).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("some-raw-token")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refresh_withExpiredToken_throwsUnauthorized() {
        RefreshToken stored = new RefreshToken();
        stored.setUser(adminUser);
        stored.setRevoked(false);
        stored.setExpiresAt(Instant.now().minusSeconds(3600));

        when(refreshTokenRepository.findOne(any(Specification.class))).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("some-raw-token")))
                .isInstanceOf(UnauthorizedException.class);
    }
}
