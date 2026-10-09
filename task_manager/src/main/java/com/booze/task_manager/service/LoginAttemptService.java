package com.booze.task_manager.service;

import com.booze.task_manager.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final UserRepo userRepo;

    // REQUIRES_NEW: the counter must be saved even though the login request itself fails.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String email) {
        userRepo.findByEmailIgnoreCase(email.toLowerCase(Locale.ROOT)).ifPresent(user -> {
            Instant now = Instant.now();
            if (user.getLockedUntil() != null && user.getLockedUntil().isBefore(now)) {
                user.setLockedUntil(null);
                user.setFailedLoginAttempts(0);
            }
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_ATTEMPTS) {
                user.setLockedUntil(now.plus(LOCK_DURATION));
            }
        });
    }

    @Transactional
    public void recordSuccess(String userId) {
        userRepo.findById(userId).ifPresent(user -> {
            if (user.getFailedLoginAttempts() != 0 || user.getLockedUntil() != null) {
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
            }
        });
    }
}