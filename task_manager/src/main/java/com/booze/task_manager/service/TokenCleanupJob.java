package com.booze.task_manager.service;

import com.booze.task_manager.repo.RefreshTokenRepo;
import com.booze.task_manager.repo.RevokedTokenRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupJob {
    private final RevokedTokenRepo revokedTokenRepo;
    private final RefreshTokenRepo refreshTokenRepo;

    @Scheduled(cron = "0 15 * * * *")
    @Transactional
    public void purge() {
        Instant now = Instant.now();
        int a = revokedTokenRepo.deleteExpired(now);
        // keep expired refresh rows a week so "reuse of an old token" is still detected
        int b = refreshTokenRepo.deleteExpired(now.minus(Duration.ofDays(7)));
        log.info("Token cleanup removed {} revoked access tokens and {} refresh tokens", a, b);
    }
}