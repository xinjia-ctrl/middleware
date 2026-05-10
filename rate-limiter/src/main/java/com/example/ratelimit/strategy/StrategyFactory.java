package com.example.ratelimit.strategy;

import com.example.ratelimit.annotation.RateLimit;
import com.example.ratelimit.strategy.redis.*;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StrategyFactory {

    private final Map<String, RateLimitStrategy> strategyCache = new ConcurrentHashMap<>();
    private final StringRedisTemplate redisTemplate;

    public StrategyFactory(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public RateLimitStrategy getStrategy(RateLimit annotation) {
        String cacheKey = annotation.mode().name() + ":" + buildConfigKey(annotation);
        return strategyCache.computeIfAbsent(cacheKey, k -> createStrategy(annotation));
    }

    private String buildConfigKey(RateLimit annotation) {
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
        if (annotation.mode() == RateLimitMode.DISTRIBUTED) {
            return createRedisStrategy(annotation);
        }
        return createLocalStrategy(annotation);
    }

    private RateLimitStrategy createRedisStrategy(RateLimit annotation) {
        if (redisTemplate == null) {
            throw new IllegalStateException("RedisTemplate is required for DISTRIBUTED mode but Redis is not configured");
        }
        return switch (annotation.strategy()) {
            case FIXED_WINDOW ->
                    new RedisFixedWindowStrategy(redisTemplate, annotation.permits(), annotation.timeUnit().toMillis(annotation.window()));
            case SLIDING_WINDOW ->
                    new RedisSlidingWindowStrategy(redisTemplate, annotation.permits(), annotation.timeUnit().toMillis(annotation.window()));
            case TOKEN_BUCKET ->
                    new RedisTokenBucketStrategy(redisTemplate, annotation.permitsPerSecond(), annotation.capacity());
            case LEAKY_BUCKET ->
                    new RedisLeakyBucketStrategy(redisTemplate, annotation.leakRate(), annotation.leakCapacity());
        };
    }

    private RateLimitStrategy createLocalStrategy(RateLimit annotation) {
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
