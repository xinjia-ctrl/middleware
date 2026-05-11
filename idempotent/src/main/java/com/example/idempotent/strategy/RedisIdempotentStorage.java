package com.example.idempotent.strategy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

public class RedisIdempotentStorage implements IdempotentStorage {

    private static final String LOCK_KEY_PREFIX = "idempotent:";
    private static final String RESULT_KEY_PREFIX = "idempotent:result:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisIdempotentStorage(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public boolean trySave(String key, long ttl, TimeUnit timeUnit) {
        String redisKey = LOCK_KEY_PREFIX + key;
        Boolean saved = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "1", ttl, timeUnit);
        return Boolean.TRUE.equals(saved);
    }

    @Override
    public void remove(String key) {
        redisTemplate.delete(LOCK_KEY_PREFIX + key);
        redisTemplate.delete(RESULT_KEY_PREFIX + key);
    }

    @Override
    public void saveResult(String key, Object result, long ttl, TimeUnit timeUnit) {
        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(RESULT_KEY_PREFIX + key, json, ttl, timeUnit);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化幂等结果失败", e);
        }
    }

    @Override
    public Object getResult(String key) {
        String json = redisTemplate.opsForValue().get(RESULT_KEY_PREFIX + key);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
