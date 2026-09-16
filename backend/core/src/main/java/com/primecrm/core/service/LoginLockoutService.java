package com.primecrm.core.service;

import com.primecrm.core.audit.AuditService;
import com.primecrm.infra.entity.audit.AuditAction;
import com.primecrm.infra.entity.auth.User;
import com.primecrm.infra.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LoginLockoutService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final AuditService auditService;

    public boolean isLocked(User user) {
        return user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerFailure(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(Instant.now().plus(LOCKOUT_DURATION));
                userRepository.save(user);
                auditService.recordSecurityEvent(AuditAction.LOGIN_LOCKED, user.getId(), user.getEmail(),
                        Map.of("failedAttempts", MAX_FAILED_ATTEMPTS, "lockedUntil", user.getLockedUntil().toString()));
            } else {
                user.setFailedLoginAttempts(attempts);
                userRepository.save(user);
            }
        });
    }
}
