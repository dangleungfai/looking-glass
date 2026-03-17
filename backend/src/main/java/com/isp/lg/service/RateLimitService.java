package com.isp.lg.service;

import com.isp.lg.repository.SystemSettingRepository;
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

    private final SystemSettingRepository systemSettingRepository;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private static final long WINDOW_MS = 60_000;

    public RateLimitService(SystemSettingRepository systemSettingRepository) {
        this.systemSettingRepository = systemSettingRepository;
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
        return systemSettingRepository.findBySettingKey("rate_limit_per_minute")
                .map(s -> {
                    try {
                        return Integer.parseInt(s.getSettingValue() != null ? s.getSettingValue().trim() : "20");
                    } catch (NumberFormatException e) {
                        return 20;
                    }
                })
                .orElse(20);
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
