package com.example.ratelimit.strategy.redis;

import com.example.ratelimit.strategy.StrategyType;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisSlidingWindowStrategy extends AbstractRedisStrategy {

    private final long maxPermits;
    private final long windowMillis;

    public RedisSlidingWindowStrategy(StringRedisTemplate redisTemplate, long maxPermits, long windowMillis) {
        super(redisTemplate, "scripts/sliding-window.lua", "rate-limiter:sliding-window");
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
        return StrategyType.SLIDING_WINDOW;
    }
}
