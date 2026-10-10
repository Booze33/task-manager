package com.booze.task_manager.model;

import com.booze.task_manager.dto.Priority;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "tasks")
@Getter
@Setter
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean isCompleted = false;

    @Column(nullable = false)
    private Instant expiresIn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
