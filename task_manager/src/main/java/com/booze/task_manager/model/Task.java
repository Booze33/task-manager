package com.booze.task_manager.model;

import com.booze.task_manager.dto.Priority;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "tasks", indexes = {
        @Index(name = "idx_tasks_owner_created", columnList = "owner_id, created_at, id"),
        @Index(name = "idx_tasks_owner_status", columnList = "owner_id, status"),
        @Index(name = "idx_tasks_owner_habit", columnList = "owner_id, habit"),
})
@Getter
@Setter
@NoArgsConstructor
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tasks_owner"))
    private User owner;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    @Column(nullable = false, length = 100)
    private String habit;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
