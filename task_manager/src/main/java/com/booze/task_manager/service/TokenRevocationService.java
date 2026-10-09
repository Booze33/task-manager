package com.booze.task_manager.service;

import com.booze.task_manager.model.RevokedToken;
import com.booze.task_manager.repo.RevokedTokenRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenRevocationService {
    private final RevokedTokenRepo repo;

    public void revoke(String jti, Instant expiresAt) {
        if (repo.existsByJti(jti)) {
            return;
        }
        try {
            repo.save(RevokedToken.builder()
                    .jti(jti)
                    .expiresAt(expiresAt)
                    .revokedAt(Instant.now())
                    .build());
        } catch (DataIntegrityViolationException alreadyRevokedByParallelRequest) {
            // Two logouts at the same moment: the unique constraint won. That's fine.
        }
    }

    public boolean isRevoked(String jti) {
        return repo.existsByJti(jti);
    }
}