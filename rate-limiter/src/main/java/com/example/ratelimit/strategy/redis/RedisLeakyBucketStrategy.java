package com.example.ratelimit.strategy.redis;

import com.example.ratelimit.strategy.StrategyType;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisLeakyBucketStrategy extends AbstractRedisStrategy {

    private final double leakRatePerSecond;
    private final int capacity;

    public RedisLeakyBucketStrategy(StringRedisTemplate redisTemplate, double leakRatePerSecond, int capacity) {
        super(redisTemplate, "scripts/leaky-bucket.lua", "rate-limiter:leaky-bucket");
        this.leakRatePerSecond = leakRatePerSecond;
        this.capacity = capacity;
    }

    @Override
    protected Object[] buildArgs(int permits) {
        return new Object[]{
                String.valueOf(leakRatePerSecond),
                String.valueOf(capacity),
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(permits)
        };
    }

    @Override
    public StrategyType getType() {
        return StrategyType.LEAKY_BUCKET;
    }
}
