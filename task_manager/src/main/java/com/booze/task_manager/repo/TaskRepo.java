package com.booze.task_manager.repo;

import com.booze.task_manager.model.Task;
import com.booze.task_manager.model.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TaskRepo extends JpaRepository<Task, String> {
    @Query("SELECT task FROM Task task WHERE task.id = :id AND task.owner.id = :ownerId")
    Optional<Task> findOwnedTask(@Param("id") String id, @Param("ownerId") String ownerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Task task
            SET task.status = :done, task.completedAt = :completedAt
            WHERE task.id = :id AND task.owner.id = :ownerId AND task.status <> :done
            """)
    int completeIfNotDone(
            @Param("id") String id,
            @Param("ownerId") String ownerId,
            @Param("done") TaskStatus done,
            @Param("completedAt") Instant completedAt);

    @Query("""
            SELECT task FROM Task task
            WHERE task.owner.id = :ownerId
              AND (:status IS NULL OR task.status = :status)
              AND (:habit IS NULL OR task.habit = :habit)
            """)
    Page<Task> findTasks(
            @Param("ownerId") String ownerId,
            @Param("status") TaskStatus status,
            @Param("habit") String habit,
            Pageable pageable);

    long countByOwner_Id(String ownerId);

    long countByOwner_IdAndStatus(String ownerId, TaskStatus status);

    @Query("SELECT task.completedAt FROM Task task WHERE task.owner.id = :ownerId AND task.completedAt IS NOT NULL")
    List<Instant> findCompletionTimes(@Param("ownerId") String ownerId);
}
