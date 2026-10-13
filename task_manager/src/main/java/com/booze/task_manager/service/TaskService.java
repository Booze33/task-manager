package com.booze.task_manager.service;

import com.booze.task_manager.dto.TaskPageResponse;
import com.booze.task_manager.dto.TaskRequest;
import com.booze.task_manager.dto.TaskResponse;
import com.booze.task_manager.exception.InvalidTaskFilterException;
import com.booze.task_manager.exception.TaskNotFoundException;
import com.booze.task_manager.model.Task;
import com.booze.task_manager.model.TaskStatus;
import com.booze.task_manager.model.User;
import com.booze.task_manager.repo.TaskRepo;
import com.booze.task_manager.repo.UserRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepo taskRepo;
    private final UserRepo userRepo;
    private final StatsCacheService statsCacheService;

    @Transactional
    public Task create(String userId, TaskRequest request) {
        User user = userRepo.findById(userId)
                .orElseThrow(TaskNotFoundException::new);

        Task task = new Task();
        task.setOwner(user);
        task.setTitle(request.title().trim());
        task.setDescription(request.description() == null ? "" : request.description().trim());
        task.setHabit(request.habit().trim());
        task.setDueDate(request.dueDate());
        task.setStatus(TaskStatus.TODO);
        task.setPriority(request.priority());

        Task saved = taskRepo.save(task);
        invalidateStatsAfterCommit(userId);
        return saved;
    }

    @Transactional
    public Completion complete(String userId, String taskId) {
        int updated = taskRepo.completeIfNotDone(taskId, userId, TaskStatus.DONE, Instant.now());
        Task task = taskRepo.findOwnedTask(taskId, userId).orElseThrow(TaskNotFoundException::new);

        if (updated > 0) {
            invalidateStatsAfterCommit(userId);
        }
        return new Completion(task, updated == 0);
    }

    public void invalidateStatsAfterCommit(String userId){
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            statsCacheService.invalidate(userId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                statsCacheService.invalidate(userId);
            }
        });
    }

    public TaskPageResponse list(String userId, int page, int size, String rawStatus, String rawHabit) {
        TaskStatus status = parseStatus(rawStatus);
        String habit = normalizeFilter(rawHabit);

        Page<Task> result = taskRepo.findTasks(
                userId,
                status,
                habit,
                PageRequest.of(page - 1, size, Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id"))));

        return new TaskPageResponse(
                result.getContent().stream().map(TaskResponse::from).toList(),
                result.getTotalElements(),
                page,
                size,
                result.getTotalPages()
        );
    }

    private static TaskStatus parseStatus(String rawStatus) {
        String status = normalizeFilter(rawStatus);
        if (status == null) {
            return null;
        }
        try {
            return TaskStatus.fromValue(status);
        } catch (IllegalArgumentException exception) {
            throw new InvalidTaskFilterException(status);
        }
    }

    private static String normalizeFilter(String filter) {
        if (filter == null || filter.isBlank()) {
            return null;
        }
        return filter.trim();
    }

    public record Completion(Task task, boolean idempotent) {
    }
}
