package com.primecrm.api.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InMemoryRateLimiter {

    private static final long IDLE_EVICTION_SECONDS = 600;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public boolean tryConsume(String key, int capacity, long windowSeconds) {
        long nowEpoch = Instant.now().getEpochSecond();
        long currentWindowId = nowEpoch / windowSeconds;
        Window window = windows.computeIfAbsent(key, ignored -> new Window());

        synchronized (window) {
            window.lastAccessEpoch = nowEpoch;
            if (window.windowId != currentWindowId) {
                window.windowId = currentWindowId;
                window.count = 0;
            }
            if (window.count >= capacity) {
                return false;
            }
            window.count++;
            return true;
        }
    }

    @Scheduled(fixedRate = 300_000)
    public void evictIdleWindows() {
        long now = Instant.now().getEpochSecond();
        windows.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                return now - entry.getValue().lastAccessEpoch > IDLE_EVICTION_SECONDS;
            }
        });
    }

    private static final class Window {
        long windowId;
        int count;
        long lastAccessEpoch;
    }
}
