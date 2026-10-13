package com.booze.task_manager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RateLimitService {
    private static final long LIMIT = 100;
    private static final long WINDOW_SECONDS = 60;
    private static final DefaultRedisScript<List> INCREMENT_SCRIPT = new DefaultRedisScript<>(
            """
                    local count = redis.call('INCR', KEYS[1])
                    if count == 1 then
                      redis.call('EXPIRE', KEYS[1], ARGV[1])
                    end
                    return {count, redis.call('TTL', KEYS[1])}
                    """,
            List.class);

    private final StringRedisTemplate redis;

    public RateLimitResult consume(String identity) {
        long now = Instant.now().getEpochSecond();
        List<?> result = redis.execute(
                INCREMENT_SCRIPT,
                List.of("task-manager:rate-limit:" + identity),
                Long.toString(WINDOW_SECONDS));
        if (result == null || result.size() != 2) {
            throw new IllegalStateException("Redis returned an invalid rate-limit result");
        }

        long count = number(result.get(0));
        long ttl = number(result.get(1));
        long remaining = Math.max(0, LIMIT - count);
        return new RateLimitResult(count <= LIMIT, remaining, now + Math.max(0, ttl));
    }

    private static long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            return Long.parseLong(text);
        }
        throw new IllegalStateException("Redis returned a non-numeric rate-limit value");
    }

    public record RateLimitResult(boolean allowed, long remaining, long resetEpochSeconds) {
    }
}
