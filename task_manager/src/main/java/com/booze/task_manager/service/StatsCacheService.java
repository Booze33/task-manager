package com.booze.task_manager.service;

import com.booze.task_manager.dto.UserStatsResponse;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@Service
public class StatsCacheService {
    private static final Duration TTL = Duration.ofSeconds(30);
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public CachedStats get(String userId, Supplier<UserStatsResponse> loader) {
        AtomicBoolean hit = new AtomicBoolean();
        CacheEntry entry = cache.compute(userId, (key, cached) -> {
            Instant now = Instant.now();
            if (cached != null && cached.expiresAt().isAfter(now)) {
                hit.set(true);
                return cached;
            }
            return new CacheEntry(loader.get(), now.plus(TTL));
        });
        return new CachedStats(entry.stats(), hit.get());
    }

    public void invalidate(String userId) {
        cache.remove(userId);
    }

    public record CachedStats(UserStatsResponse stats, boolean hit) {
    }

    private record CacheEntry(UserStatsResponse stats, Instant expiresAt) {
    }
}
