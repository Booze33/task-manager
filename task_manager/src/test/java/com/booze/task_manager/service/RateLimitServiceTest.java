package com.booze.task_manager.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RateLimitServiceTest {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final RateLimitService service = new RateLimitService(redis);

    @Test
    void consumesQuotaAndReturnsRemainingAndReset() {
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(List.of(1L, 60L));

        var result = service.consume("user:user-1");

        assertTrue(result.allowed());
        assertEquals(99, result.remaining());
        assertTrue(result.resetEpochSeconds() > 0);
    }

    @Test
    void deniesRequestOnceLimitIsExceeded() {
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(List.of(101L, 12L));

        var result = service.consume("ip:127.0.0.1");

        assertFalse(result.allowed());
        assertEquals(0, result.remaining());
    }
}
