package com.booze.task_manager.service;

import com.booze.task_manager.exception.InvalidRefreshTokenException;
import com.booze.task_manager.model.RefreshToken;
import com.booze.task_manager.repo.RefreshTokenRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepo repo;
    private final Duration ttl;

    public RefreshTokenService(RefreshTokenRepo repo,
                               @Value("${app.jwt.refresh-token-expiration}") long ttlMs) {
        this.repo = repo;
        this.ttl = Duration.ofMillis(ttlMs);
    }

    public record Rotated(String userId, String newRawToken) {
    }

    @Transactional
    public String issueNewFamily(String userId) {
        return issue(userId, UUID.randomUUID().toString());
    }

    /**
     * Refresh-token ROTATION with REUSE DETECTION.
     * Every refresh token works exactly once. If a token that was already used shows up again,
     * someone copied it (attacker or victim is replaying it), so we kill the entire family.
     *
     * noRollbackFor is critical: without it, throwing the exception would roll back the
     * family revocation we just did, and the "security response" would silently vanish.
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public Rotated rotate(String rawToken) {
        RefreshToken current = repo.findByTokenHash(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);
        Instant now = Instant.now();

        if (current.getRevokedAt() != null) {
            repo.revokeFamily(current.getFamilyId(), now);
            throw new InvalidRefreshTokenException();
        }
        if (current.getExpiresAt().isBefore(now)) {
            throw new InvalidRefreshTokenException();
        }

        current.setRevokedAt(now);
        String next = issue(current.getUserId(), current.getFamilyId());
        return new Rotated(current.getUserId(), next);
    }

    @Transactional
    public void revokeFamilyOf(String rawToken) {
        repo.findByTokenHash(hash(rawToken))
                .ifPresent(t -> repo.revokeFamily(t.getFamilyId(), Instant.now()));
    }

    @Transactional
    public void revokeAllForUser(String userId) {
        repo.revokeAllForUser(userId, Instant.now());
    }

    private String issue(String userId, String familyId) {
        byte[] bytes = new byte[32]; // 256 bits of randomness
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        Instant now = Instant.now();
        repo.save(RefreshToken.builder()
                .tokenHash(hash(raw))
                .userId(userId)
                .familyId(familyId)
                .createdAt(now)
                .expiresAt(now.plus(ttl))
                .build());
        return raw;
    }

    private static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}