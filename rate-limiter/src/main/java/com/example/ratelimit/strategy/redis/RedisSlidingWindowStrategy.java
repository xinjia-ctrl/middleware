package com.example.ratelimit.strategy.redis;

import com.example.ratelimit.strategy.RateLimitStrategy;
import com.example.ratelimit.strategy.StrategyType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.List;

public class RedisSlidingWindowStrategy implements RateLimitStrategy {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> script;
    private final long maxPermits;
    private final long windowMillis;

    public RedisSlidingWindowStrategy(StringRedisTemplate redisTemplate, long maxPermits, long windowMillis) {
        this.redisTemplate = redisTemplate;
        this.maxPermits = maxPermits;
        this.windowMillis = windowMillis;
        this.script = new DefaultRedisScript<>();
        this.script.setResultType(Long.class);
        this.script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("scripts/sliding-window.lua")));
    }

    @Override
    public boolean tryAcquire(String key, int permits) {
        String redisKey = "rate-limiter:sliding-window:" + key;
        Long result = redisTemplate.execute(script, List.of(redisKey),
                String.valueOf(maxPermits),
                String.valueOf(windowMillis),
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(permits));
        return result != null && result == 1;
    }

    @Override
    public StrategyType getType() {
        return StrategyType.SLIDING_WINDOW;
    }
}
