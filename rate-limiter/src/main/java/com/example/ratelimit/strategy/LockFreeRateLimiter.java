package com.example.ratelimit.strategy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * CAS 无锁限流器，基于时间片抢占
 * 每个 key 维护一个 AtomicLong 记录下一次可用的时间戳
 * compareAndSet 抢占时间片，无锁无阻塞，无后台线程
 */
public class LockFreeRateLimiter implements RateLimitStrategy {

    private final long intervalNs;      // 每个 permit 的时间间隔（纳秒）
    private final long maxQueueNs;      // 最大排队等待窗口
    private final int maxRetries;
    private final Map<String, AtomicLong> limiters = new ConcurrentHashMap<>();

    public LockFreeRateLimiter(double permitsPerSecond) {
        this(permitsPerSecond, 3);
    }

    public LockFreeRateLimiter(double permitsPerSecond, int maxQueuePermits) {
        this(permitsPerSecond, maxQueuePermits, 100);
    }

    public LockFreeRateLimiter(double permitsPerSecond, int maxQueuePermits, int maxRetries) {
        this.intervalNs = (long) (1_000_000_000d / permitsPerSecond);
        this.maxQueueNs = (long) intervalNs * maxQueuePermits;
        this.maxRetries = maxRetries;
    }

    @Override
    public boolean tryAcquire(String key, int permits) {
        AtomicLong next = limiters.computeIfAbsent(key, k -> new AtomicLong(0));
        long now = System.nanoTime();
        long cost = intervalNs * permits;

        for (int i = 0; i < maxRetries; i++) {
            long pre = next.get();
            long waitTime = pre - now;
            if (waitTime > maxQueueNs) {
                return false;
            }
            long newVal = Math.max(pre, now) + cost;
            if (next.compareAndSet(pre, newVal)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public StrategyType getType() {
        return StrategyType.LOCK_FREE;
    }
}
