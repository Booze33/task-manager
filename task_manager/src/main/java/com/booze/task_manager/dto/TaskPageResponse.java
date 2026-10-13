package com.booze.task_manager.dto;

import java.util.List;

public record TaskPageResponse(
        List<TaskResponse> items,
        long total,
        int page,
        int size,
        int totalPages
) {
}
