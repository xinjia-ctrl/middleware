package com.example.ratelimit.strategy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LeakyBucketStrategy implements RateLimitStrategy {

    private final double leakRatePerSecond;
    private final int capacity;
    private final Map<String, LeakyBucket> bucketMap = new ConcurrentHashMap<>();

    public LeakyBucketStrategy(double leakRatePerSecond, int capacity) {
        this.leakRatePerSecond = leakRatePerSecond;
        this.capacity = capacity;
    }

    @Override
    public boolean tryAcquire(String key, int permits) {
        long now = System.nanoTime();
        LeakyBucket bucket = bucketMap.computeIfAbsent(key, k -> new LeakyBucket(capacity, now));

        synchronized (bucket) {
            bucket.leak(now, leakRatePerSecond);
            if (bucket.water + permits <= capacity) {
                bucket.water += permits;
                return true;
            }
            return false;
        }
    }

    @Override
    public StrategyType getType() {
        return StrategyType.LEAKY_BUCKET;
    }

    private static class LeakyBucket {
        double water;
        long lastLeakNanos;
        final int capacity;

        LeakyBucket(int capacity, long now) {
            this.capacity = capacity;
            this.water = 0;
            this.lastLeakNanos = now;
        }

        void leak(long now, double rate) {
            long elapsed = now - lastLeakNanos;
            double leaked = elapsed * rate / 1_000_000_000d;
            water = Math.max(0, water - leaked);
            lastLeakNanos = now;
        }
    }
}
