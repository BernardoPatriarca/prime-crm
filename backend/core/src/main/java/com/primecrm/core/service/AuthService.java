package com.primecrm.core.service;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.auth.ChangeOwnPasswordRequest;
import com.primecrm.core.dto.auth.LoginRequest;
import com.primecrm.core.dto.auth.LoginResponse;
import com.primecrm.core.dto.auth.MeResponse;
import com.primecrm.core.dto.auth.RefreshTokenRequest;
import com.primecrm.core.security.JwtTokenProvider;
import com.primecrm.core.service.support.UserAuthorityResolver;
import com.primecrm.core.specification.UserSpecifications;
import com.primecrm.infra.entity.audit.AuditAction;
import com.primecrm.infra.entity.auth.Role;
import com.primecrm.infra.entity.auth.User;
import com.primecrm.infra.entity.auth.UserStatus;
import com.primecrm.infra.entity.auth.RefreshToken;
import com.primecrm.infra.repository.RefreshTokenRepository;
import com.primecrm.infra.repository.UserRepository;
import com.primecrm.core.specification.RefreshTokenSpecifications;
import com.primecrm.shared.exception.ResourceNotFoundException;
import com.primecrm.shared.exception.UnauthorizedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int REFRESH_TOKEN_BYTES = 32;

    private static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    private static final String USER_NOT_ACTIVE = "USER_NOT_ACTIVE";
    private static final String INVALID_REFRESH_TOKEN = "INVALID_REFRESH_TOKEN";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserAuthorityResolver authorityResolver;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findOne(UserSpecifications.byLoginOrEmail(request.usernameOrEmail()))
                .orElse(null);

        if (user == null || user.isDeleted()) {
            throw loginFailure(null, request.usernameOrEmail(), INVALID_CREDENTIALS, "Credenciais invalidas");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw loginFailure(user, request.usernameOrEmail(), INVALID_CREDENTIALS, "Credenciais invalidas");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw loginFailure(user, request.usernameOrEmail(), USER_NOT_ACTIVE, "Usuario inativo ou bloqueado");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        auditService.recordSecurityEvent(AuditAction.LOGIN, user.getId(), user.getEmail(),
                Map.of("login", user.getLogin()));

        return issueTokenPair(user);
    }

    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request) {
        String hash = hash(request.refreshToken());
        RefreshToken stored = refreshTokenRepository.findOne(RefreshTokenSpecifications.byTokenHash(hash))
                .orElseThrow(() -> new UnauthorizedException(INVALID_REFRESH_TOKEN, "Refresh token invalido"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException(INVALID_REFRESH_TOKEN, "Refresh token invalido ou expirado");
        }

        User user = stored.getUser();
        if (user.isDeleted() || user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException(USER_NOT_ACTIVE, "Usuario inativo ou bloqueado");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return issueTokenPair(user);
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        String hash = hash(request.refreshToken());
        refreshTokenRepository.findOne(RefreshTokenSpecifications.byTokenHash(hash))
                .ifPresent(refreshToken -> {
                    refreshToken.setRevoked(true);
                    refreshTokenRepository.save(refreshToken);
                    User user = refreshToken.getUser();
                    auditService.recordSecurityEvent(AuditAction.LOGOUT, user.getId(), user.getEmail(), Map.of());
                });
    }

    private UnauthorizedException loginFailure(User user, String usernameOrEmail, String errorCode, String message) {
        auditService.recordSecurityEvent(AuditAction.LOGIN_FAILED,
                user == null ? null : user.getId(),
                user == null ? null : user.getEmail(),
                Map.of("usernameOrEmail", usernameOrEmail, "reason", errorCode));
        return new UnauthorizedException(errorCode, message);
    }

    @Transactional
    public void changeOwnPassword(UUID userId, ChangeOwnPasswordRequest request) {
        User user = userRepository.findById(userId)
                .filter(candidate -> !candidate.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException(INVALID_CREDENTIALS, "Senha atual invalida");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        List<RefreshToken> activeTokens = refreshTokenRepository
                .findAll(RefreshTokenSpecifications.activeByUserId(userId));
        activeTokens.forEach(token -> token.setRevoked(true));
        refreshTokenRepository.saveAll(activeTokens);

        auditService.recordSecurityEvent(AuditAction.PASSWORD_CHANGED, userId, user.getEmail(), Map.of());
    }

    @Transactional(readOnly = true)
    public MeResponse me(UUID userId) {
        User user = userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId));
        List<Role> roles = authorityResolver.resolveActiveRoles(user.getId());
        return buildMeResponse(user, roles);
    }

    private LoginResponse issueTokenPair(User user) {
        List<Role> roles = authorityResolver.resolveActiveRoles(user.getId());
        List<String> roleNames = authorityResolver.resolveRoleNames(roles);
        List<String> permissionCodes = authorityResolver.resolvePermissionCodes(roles);

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getLogin(), user.getName(), roleNames, permissionCodes);
        String rawRefreshToken = generateAndStoreRefreshToken(user);

        MeResponse meResponse = buildMeResponse(user, roles);
        return LoginResponse.of(accessToken, rawRefreshToken, jwtTokenProvider.getAccessTokenExpirationSeconds(),
                meResponse);
    }

    private MeResponse buildMeResponse(User user, List<Role> roles) {
        List<String> roleNames = authorityResolver.resolveRoleNames(roles);
        List<String> permissionCodes = authorityResolver.resolvePermissionCodes(roles);
        return new MeResponse(
                user.getId(), user.getName(), user.getEmail(), user.getLogin(), user.getStatus(),
                user.getLastLoginAt(), roleNames, permissionCodes
        );
    }

    private String generateAndStoreRefreshToken(User user) {
        byte[] randomBytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hash(rawToken));
        refreshToken.setExpiresAt(Instant.now().plus(jwtTokenProvider.getRefreshTokenExpirationDays(), ChronoUnit.DAYS));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponivel", e);
        }
    }
}
