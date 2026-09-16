package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.audit.AuditService;
import com.primecrm.infra.entity.audit.AuditAction;
import com.primecrm.infra.entity.auth.User;
import com.primecrm.infra.repository.UserRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginLockoutServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditService auditService;

    private LoginLockoutService loginLockoutService;

    private User user;

    @BeforeEach
    void setUp() {
        loginLockoutService = new LoginLockoutService(userRepository, auditService);
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("admin@primecrm.local");
    }

    @Test
    void isLocked_withFutureLockedUntil_returnsTrue() {
        user.setLockedUntil(Instant.now().plusSeconds(60));
        assertThat(loginLockoutService.isLocked(user)).isTrue();
    }

    @Test
    void isLocked_withPastLockedUntil_returnsFalse() {
        user.setLockedUntil(Instant.now().minusSeconds(60));
        assertThat(loginLockoutService.isLocked(user)).isFalse();
    }

    @Test
    void isLocked_withNoLockedUntil_returnsFalse() {
        assertThat(loginLockoutService.isLocked(user)).isFalse();
    }

    @Test
    void registerFailure_belowThreshold_incrementsCounterWithoutLocking() {
        user.setFailedLoginAttempts(1);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        loginLockoutService.registerFailure(user.getId());

        assertThat(user.getFailedLoginAttempts()).isEqualTo(2);
        assertThat(user.getLockedUntil()).isNull();
        verify(userRepository).save(user);
        verify(auditService, never()).recordSecurityEvent(
                org.mockito.ArgumentMatchers.eq(AuditAction.LOGIN_LOCKED), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyMap());
    }

    @Test
    void registerFailure_reachingThreshold_locksAccountAndResetsCounter() {
        user.setFailedLoginAttempts(4);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        loginLockoutService.registerFailure(user.getId());

        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockedUntil()).isAfter(Instant.now());
        verify(userRepository).save(user);
        verify(auditService, times(1)).recordSecurityEvent(
                org.mockito.ArgumentMatchers.eq(AuditAction.LOGIN_LOCKED),
                org.mockito.ArgumentMatchers.eq(user.getId()),
                org.mockito.ArgumentMatchers.eq(user.getEmail()),
                org.mockito.ArgumentMatchers.any(Map.class));
    }
}
