package com.example.ratelimit.strategy.redis;

import com.example.ratelimit.strategy.RateLimitStrategy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.List;

public abstract class AbstractRedisStrategy implements RateLimitStrategy {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> script;
    private final String keyPrefix;

    protected AbstractRedisStrategy(StringRedisTemplate redisTemplate, String scriptPath, String keyPrefix) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = keyPrefix;
        this.script = new DefaultRedisScript<>();
        this.script.setResultType(Long.class);
        this.script.setScriptSource(new ResourceScriptSource(new ClassPathResource(scriptPath)));
    }

    @Override
    public boolean tryAcquire(String key, int permits) {
        Long result = redisTemplate.execute(script, List.of(keyPrefix + ":" + key), buildArgs(permits));
        return result != null && result == 1L;
    }

    protected abstract Object[] buildArgs(int permits);
}
