package com.isp.lg.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory rate limiter per IP for public query API.
 * Limit: max requests per minute per IP (from system_settings or default 20).
 */
@Service
public class RateLimitService {

    private final SystemSettingsService systemSettingsService;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private static final long WINDOW_MS = 60_000;

    public RateLimitService(SystemSettingsService systemSettingsService) {
        this.systemSettingsService = systemSettingsService;
    }

    public boolean allow(String clientIp) {
        int limit = getLimitPerMinute();
        long now = System.currentTimeMillis();
        Window w = windows.compute(clientIp, (k, v) -> {
            if (v == null || now - v.startMs > WINDOW_MS) {
                return new Window(now, new AtomicInteger(0));
            }
            return v;
        });
        int count = w.count.incrementAndGet();
        if (count > limit) {
            return false;
        }
        return true;
    }

    private int getLimitPerMinute() {
        Integer value = systemSettingsService.getRateLimit().getPerIpPerMinute();
        return value != null && value > 0 ? value : 20;
    }

    private static class Window {
        final long startMs;
        final AtomicInteger count;

        Window(long startMs, AtomicInteger count) {
            this.startMs = startMs;
            this.count = count;
        }
    }
}
