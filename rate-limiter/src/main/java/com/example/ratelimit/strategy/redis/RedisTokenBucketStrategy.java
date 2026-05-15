package com.example.ratelimit.strategy.redis;

import com.example.ratelimit.strategy.StrategyType;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisTokenBucketStrategy extends AbstractRedisStrategy {

    private final double permitsPerSecond;
    private final int capacity;

    public RedisTokenBucketStrategy(StringRedisTemplate redisTemplate, double permitsPerSecond, int capacity) {
        super(redisTemplate, "scripts/token-bucket.lua", "rate-limiter:token-bucket");
        this.permitsPerSecond = permitsPerSecond;
        this.capacity = capacity;
    }

    @Override
    protected Object[] buildArgs(int permits) {
        return new Object[]{
                String.valueOf(permitsPerSecond),
                String.valueOf(capacity),
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(permits)
        };
    }

    @Override
    public StrategyType getType() {
        return StrategyType.TOKEN_BUCKET;
    }
}
