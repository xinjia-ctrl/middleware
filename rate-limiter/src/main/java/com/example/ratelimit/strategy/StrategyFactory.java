package com.example.ratelimit.strategy;

import com.example.ratelimit.annotation.RateLimit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StrategyFactory {

    private final Map<String, RateLimitStrategy> strategyCache = new ConcurrentHashMap<>();

    public RateLimitStrategy getStrategy(RateLimit annotation) {
        String cacheKey = buildCacheKey(annotation);
        return strategyCache.computeIfAbsent(cacheKey, k -> createStrategy(annotation));
    }

    private String buildCacheKey(RateLimit annotation) {
        return annotation.strategy().name() + ":" +
                annotation.permits() + ":" +
                annotation.window() + ":" +
                annotation.timeUnit() + ":" +
                annotation.permitsPerSecond() + ":" +
                annotation.capacity() + ":" +
                annotation.leakRate() + ":" +
                annotation.leakCapacity();
    }

    private RateLimitStrategy createStrategy(RateLimit annotation) {
        return switch (annotation.strategy()) {
            case FIXED_WINDOW ->
                    new FixedWindowStrategy(annotation.permits(), annotation.window(), annotation.timeUnit());
            case SLIDING_WINDOW ->
                    new SlidingWindowStrategy(annotation.permits(), annotation.window(), annotation.timeUnit());
            case TOKEN_BUCKET ->
                    new TokenBucketStrategy(annotation.permitsPerSecond(), annotation.capacity());
            case LEAKY_BUCKET ->
                    new LeakyBucketStrategy(annotation.leakRate(), annotation.leakCapacity());
        };
    }
}
