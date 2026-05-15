package com.example.ratelimit.strategy.redis;

import com.example.ratelimit.strategy.StrategyType;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisFixedWindowStrategy extends AbstractRedisStrategy {

    private final long maxPermits;
    private final long windowMillis;

    public RedisFixedWindowStrategy(StringRedisTemplate redisTemplate, long maxPermits, long windowMillis) {
        super(redisTemplate, "scripts/fixed-window.lua", "rate-limiter:fixed-window");
        this.maxPermits = maxPermits;
        this.windowMillis = windowMillis;
    }

    @Override
    protected Object[] buildArgs(int permits) {
        return new Object[]{
                String.valueOf(maxPermits),
                String.valueOf(windowMillis),
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(permits)
        };
    }

    @Override
    public StrategyType getType() {
        return StrategyType.FIXED_WINDOW;
    }
}
