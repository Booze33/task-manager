package com.booze.task_manager.service;

import com.booze.task_manager.dto.UserStatsResponse;
import com.booze.task_manager.model.TaskStatus;
import com.booze.task_manager.repo.TaskRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserStatsService {
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);

    private final TaskRepo taskRepo;
    private final StatsCacheService statsCacheService;

    @Transactional(readOnly = true)
    public StatsCacheService.CachedStats get(String userId) {
        return statsCacheService.get(userId, () -> calculate(userId));
    }

    @Transactional(readOnly = true)
    public UserStatsResponse calculate(String userId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<Instant> completionTimes = taskRepo.findCompletionTimes(userId);
        Map<LocalDate, Long> completedByDate = completionTimes.stream()
                .map(completedAt -> completedAt.atZone(ZoneOffset.UTC).toLocalDate())
                .collect(Collectors.groupingBy(date -> date, Collectors.counting()));

        long totalTasks = taskRepo.countByOwner_Id(userId);
        long completedTasks = taskRepo.countByOwner_IdAndStatus(userId, TaskStatus.DONE);
        int completionRate = totalTasks == 0 ? 0 : (int) Math.round(completedTasks * 100.0 / totalTasks);

        List<UserStatsResponse.DailyCompletion> weekly = new ArrayList<>(7);
        for (int offset = 6; offset >= 0; offset--) {
            LocalDate date = today.minusDays(offset);
            weekly.add(new UserStatsResponse.DailyCompletion(
                    DAY_LABEL.format(date),
                    completedByDate.getOrDefault(date, 0L)));
        }

        Set<LocalDate> completionDates = new HashSet<>(completedByDate.keySet());
        LocalDate streakDate = completionDates.contains(today) ? today : today.minusDays(1);
        long currentStreak = 0;
        while (completionDates.contains(streakDate)) {
            currentStreak++;
            streakDate = streakDate.minusDays(1);
        }

        return new UserStatsResponse(
                completedByDate.getOrDefault(today, 0L),
                totalTasks,
                completionRate,
                currentStreak,
                List.copyOf(weekly));
    }

    public void invalidate(String userId) {
        statsCacheService.invalidate(userId);
    }
}
