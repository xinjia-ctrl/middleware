package com.example.idempotent.strategy;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

public class RedisIdempotentStorage implements IdempotentStorage {

    private static final String KEY_PREFIX = "idempotent:";

    private final StringRedisTemplate redisTemplate;

    public RedisIdempotentStorage(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean trySave(String key, long ttl, TimeUnit timeUnit) {
        String redisKey = KEY_PREFIX + key;
        Boolean saved = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "1", ttl, timeUnit);
        return Boolean.TRUE.equals(saved);
    }

    @Override
    public void remove(String key) {
        redisTemplate.delete(KEY_PREFIX + key);
    }
}
