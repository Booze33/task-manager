package com.booze.task_manager.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "refresh_tokens",
        uniqueConstraints = {@UniqueConstraint(name = "uk_refresh_token_hash", columnNames = "token_hash")},
        indexes = {
                @Index(name = "idx_refresh_family", columnList = "family_id"),
                @Index(name = "idx_refresh_user", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "family_id", nullable = false, length = 36)
    private String familyId;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant revokedAt;
}
