package com.example.ratelimit.strategy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class FixedWindowStrategy implements RateLimitStrategy {

    private final long maxPermits;
    private final long windowMillis;
    private final Map<String, Window> windowMap = new ConcurrentHashMap<>();

    public FixedWindowStrategy(long maxPermits, long window, TimeUnit timeUnit) {
        this.maxPermits = maxPermits;
        this.windowMillis = timeUnit.toMillis(window);
    }

    @Override
    public boolean tryAcquire(String key, int permits) {
        long now = System.currentTimeMillis();
        Window window = windowMap.computeIfAbsent(key, k -> new Window(now, new AtomicLong(0)));

        synchronized (window) {
            if (now - window.windowStart >= windowMillis) {
                window.windowStart = now;
                window.counter.set(permits);
                return true;
            }
            long count = window.counter.get();
            if (count + permits <= maxPermits) {
                window.counter.addAndGet(permits);
                return true;
            }
            return false;
        }
    }

    @Override
    public StrategyType getType() {
        return StrategyType.FIXED_WINDOW;
    }

    private static class Window {
        volatile long windowStart;
        final AtomicLong counter;

        Window(long windowStart, AtomicLong counter) {
            this.windowStart = windowStart;
            this.counter = counter;
        }
    }
}
