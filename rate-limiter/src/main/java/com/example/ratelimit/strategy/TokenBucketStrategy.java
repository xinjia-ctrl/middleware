package com.example.ratelimit.strategy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 令牌桶限流策略
 * 以固定速率向桶中添加令牌，请求需获取令牌后才能通过
 * 支持一定程度的突发流量
 */
public class TokenBucketStrategy implements RateLimitStrategy {

    private final double permitsPerSecond;
    private final int capacity;
    private final Map<String, TokenBucket> bucketMap = new ConcurrentHashMap<>();

    public TokenBucketStrategy(double permitsPerSecond, int capacity) {
        this.permitsPerSecond = permitsPerSecond;
        this.capacity = capacity;
    }

    @Override
    public boolean tryAcquire(String key, int permits) {
        long now = System.nanoTime();
        TokenBucket bucket = bucketMap.computeIfAbsent(key, k -> new TokenBucket(capacity, now));

        synchronized (bucket) {
            bucket.refill(now, permitsPerSecond);
            if (bucket.tokens >= permits) {
                bucket.tokens -= permits;
                return true;
            }
            return false;
        }
    }

    @Override
    public StrategyType getType() {
        return StrategyType.TOKEN_BUCKET;
    }

    private static class TokenBucket {
        double tokens;
        long lastRefillNanos;
        final int capacity;

        TokenBucket(int capacity, long now) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.lastRefillNanos = now;
        }

        void refill(long now, double rate) {
            long elapsed = now - lastRefillNanos;
            double newTokens = elapsed * rate / 1_000_000_000d;
            tokens = Math.min(capacity, tokens + newTokens);
            lastRefillNanos = now;
        }
    }
}
