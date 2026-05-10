package com.example.ratelimit.strategy;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SlidingWindowStrategy implements RateLimitStrategy {

    private final long maxPermits;
    private final long windowMillis;
    private final Map<String, SlidingWindow> windowMap = new ConcurrentHashMap<>();
    private final Lock lock = new ReentrantLock();

    public SlidingWindowStrategy(long maxPermits, long window, TimeUnit timeUnit) {
        this.maxPermits = maxPermits;
        this.windowMillis = timeUnit.toMillis(window);
    }

    @Override
    public boolean tryAcquire(String key, int permits) {
        long now = System.currentTimeMillis();
        SlidingWindow window = windowMap.computeIfAbsent(key, k -> new SlidingWindow());

        lock.lock();
        try {
            window.cleanup(now - windowMillis);
            if (window.totalCount() + permits <= maxPermits) {
                window.add(now, permits);
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public StrategyType getType() {
        return StrategyType.SLIDING_WINDOW;
    }

    private static class SlidingWindow {
        private final TreeMap<Long, Integer> slices = new TreeMap<>();
        private int total;

        void add(long timestamp, int count) {
            slices.merge(timestamp, count, Integer::sum);
            total += count;
        }

        void cleanup(long boundary) {
            var head = slices.headMap(boundary, false);
            for (int count : head.values()) {
                total -= count;
            }
            head.clear();
        }

        int totalCount() {
            return total;
        }
    }
}
