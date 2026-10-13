package com.booze.task_manager.dto;

import java.util.List;

public record UserStatsResponse(
        long completedToday,
        long totalTasks,
        int completionRate,
        long currentStreak,
        List<DailyCompletion> weekly
) {
    public record DailyCompletion(String day, long value) {
    }
}
