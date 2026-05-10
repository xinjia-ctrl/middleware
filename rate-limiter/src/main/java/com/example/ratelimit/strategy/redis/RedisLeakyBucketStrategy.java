package com.example.ratelimit.strategy.redis;

import com.example.ratelimit.strategy.RateLimitStrategy;
import com.example.ratelimit.strategy.StrategyType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.List;

public class RedisLeakyBucketStrategy implements RateLimitStrategy {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> script;
    private final double leakRatePerSecond;
    private final int capacity;

    public RedisLeakyBucketStrategy(StringRedisTemplate redisTemplate, double leakRatePerSecond, int capacity) {
        this.redisTemplate = redisTemplate;
        this.leakRatePerSecond = leakRatePerSecond;
        this.capacity = capacity;
        this.script = new DefaultRedisScript<>();
        this.script.setResultType(Long.class);
        this.script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("scripts/leaky-bucket.lua")));
    }

    @Override
    public boolean tryAcquire(String key, int permits) {
        String redisKey = "rate-limiter:leaky-bucket:" + key;
        Long result = redisTemplate.execute(script, List.of(redisKey),
                String.valueOf(leakRatePerSecond),
                String.valueOf(capacity),
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(permits));
        return result != null && result == 1;
    }

    @Override
    public StrategyType getType() {
        return StrategyType.LEAKY_BUCKET;
    }
}
