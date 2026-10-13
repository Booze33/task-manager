package com.booze.task_manager.dto;

import com.booze.task_manager.model.Task;
import com.booze.task_manager.model.TaskStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(
        String id,
        String title,
        String description,
        String status,
        String habit,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        LocalDate dueDate,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant createdAt,
        String priority
) {
    public static TaskResponse from(Task task) {
        TaskStatus status = task.getStatus();
        Priority priority = task.getPriority();
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                status.value(),
                task.getHabit(),
                task.getDueDate(),
                task.getCreatedAt(),
                priority.value());
    }
}
