package com.example.idempotent.strategy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class InMemoryIdempotentStorage implements IdempotentStorage {

    private final Map<String, Long> store = new ConcurrentHashMap<>();

    @Override
    public boolean trySave(String key, long ttl, TimeUnit timeUnit) {
        long expireAt = System.currentTimeMillis() + timeUnit.toMillis(ttl);
        Long old = store.putIfAbsent(key, expireAt);
        if (old != null) {
            return false;
        }
        return true;
    }

    @Override
    public void remove(String key) {
        store.remove(key);
    }

    public void evictExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> e.getValue() <= now);
    }
}
