package com.booze.task_manager.service;

import com.booze.task_manager.dto.UserStatsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StatsCacheServiceTest {
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final StatsCacheService cache = new StatsCacheService(redis, objectMapper);

    StatsCacheServiceTest() {
        when(redis.opsForValue()).thenReturn(values);
    }

    @Test
    void missLoadsCachesForThirtySecondsAndReturnsMiss() {
        UserStatsResponse stats = new UserStatsResponse(2, 4, 50, 1, List.of());
        when(values.get("task-manager:stats:user-1")).thenReturn(null);

        var result = cache.get("user-1", () -> stats);

        assertEquals(stats, result.stats());
        assertFalse(result.hit());
        verify(values).set(eq("task-manager:stats:user-1"), anyString(), eq(Duration.ofSeconds(30)));
    }

    @Test
    void hitDeserializesCachedStatsWithoutCallingLoader() {
        String json = """
                {"completedToday":2,"totalTasks":4,"completionRate":50,"currentStreak":1,"weekly":[]}
                """;
        when(values.get("task-manager:stats:user-1")).thenReturn(json);

        var result = cache.get("user-1", () -> {
            throw new AssertionError("cache hit should not calculate stats");
        });

        assertTrue(result.hit());
        assertEquals(2, result.stats().completedToday());
        verify(values, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void invalidationDeletesOnlyThatUsersCacheKey() {
        cache.invalidate("user-1");

        verify(redis).delete("task-manager:stats:user-1");
    }
}
