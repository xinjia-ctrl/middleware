package com.example.idempotent.strategy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class InMemoryIdempotentStorage implements IdempotentStorage {

    private final Map<String, Long> store = new ConcurrentHashMap<>();
    private final Map<String, Object> resultStore = new ConcurrentHashMap<>();

    @Override
    public boolean trySave(String key, long ttl, TimeUnit timeUnit) {
        long expireAt = System.currentTimeMillis() + timeUnit.toMillis(ttl);
        Long old = store.putIfAbsent(key, expireAt);
        return old == null;
    }

    @Override
    public void remove(String key) {
        store.remove(key);
        resultStore.remove(key);
    }

    @Override
    public void saveResult(String key, Object result, long ttl, TimeUnit timeUnit) {
        resultStore.put(key, result);
    }

    @Override
    public Object getResult(String key) {
        return resultStore.get(key);
    }

    public void evictExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> e.getValue() <= now);
        resultStore.keySet().removeIf(k -> !store.containsKey(k));
    }
}
