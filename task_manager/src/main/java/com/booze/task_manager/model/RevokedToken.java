package com.booze.task_manager.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "revoked_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevokedToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String jti;
    
    @Column(nullable = false)
    private Instant expiresAt;
    
    @Column(nullable = false)
    private Instant revokedAt;
}
