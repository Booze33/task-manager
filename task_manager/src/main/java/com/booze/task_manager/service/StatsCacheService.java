package com.booze.task_manager.service;

import com.booze.task_manager.dto.UserStatsResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class StatsCacheService {
    private static final Duration TTL = Duration.ofSeconds(30);
    private static final String KEY_PREFIX = "task-manager:stats:";
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public CachedStats get(String userId, Supplier<UserStatsResponse> loader) {
        String key = key(userId);
        String cached = redis.opsForValue().get(key);
        if (cached != null) {
            try {
                return new CachedStats(objectMapper.readValue(cached, UserStatsResponse.class), true);
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Could not deserialize cached stats for user " + userId, exception);
            }
        }

        UserStatsResponse stats = loader.get();
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(stats), TTL);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize stats for user " + userId, exception);
        }
        return new CachedStats(stats, false);
    }

    public void invalidate(String userId) {
        redis.delete(key(userId));
    }

    private static String key(String userId) {
        return KEY_PREFIX + userId;
    }

    public record CachedStats(UserStatsResponse stats, boolean hit) {
    }
}
