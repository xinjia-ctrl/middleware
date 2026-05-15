package com.example.idempotent.strategy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

public class RedisIdempotentStorage implements IdempotentStorage {

    private static final String LOCK_KEY_PREFIX = "idempotent:";
    private static final String RESULT_KEY_PREFIX = "idempotent:result:";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final StringRedisTemplate redisTemplate;

    public RedisIdempotentStorage(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
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
            CacheEntry entry = new CacheEntry(result.getClass().getName(), OBJECT_MAPPER.writeValueAsString(result));
            String json = OBJECT_MAPPER.writeValueAsString(entry);
            redisTemplate.opsForValue().set(RESULT_KEY_PREFIX + key, json, ttl, timeUnit);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize idempotent result", e);
        }
    }

    @Override
    public Object getResult(String key) {
        String json = redisTemplate.opsForValue().get(RESULT_KEY_PREFIX + key);
        if (json == null) {
            return null;
        }
        try {
            CacheEntry entry = OBJECT_MAPPER.readValue(json, CacheEntry.class);
            Class<?> clazz = Class.forName(entry.type);
            return OBJECT_MAPPER.readValue(entry.data, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    private record CacheEntry(String type, String data) {
    }
}
