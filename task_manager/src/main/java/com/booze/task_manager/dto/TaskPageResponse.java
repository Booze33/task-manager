package com.booze.task_manager.dto;

import com.booze.task_manager.model.Task;

import java.util.List;

public record TaskPageResponse(
        List<Task> items,
        long total,
        int page,
        int size,
        int totalPages
) {
}
