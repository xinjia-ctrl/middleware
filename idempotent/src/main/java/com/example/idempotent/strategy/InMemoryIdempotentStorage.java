package com.example.idempotent.strategy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryIdempotentStorage implements IdempotentStorage {

    private static final int EVICT_INTERVAL = 10;

    private final Map<String, Long> lockStore = new ConcurrentHashMap<>();
    private final Map<String, ResultWithExpiry> resultStore = new ConcurrentHashMap<>();
    private final AtomicInteger opCount = new AtomicInteger(0);

    @Override
    public boolean trySave(String key, long ttl, TimeUnit timeUnit) {
        if (opCount.incrementAndGet() % EVICT_INTERVAL == 1) {
            evictExpired();
        }

        long expireAt = System.currentTimeMillis() + timeUnit.toMillis(ttl);
        Long old = lockStore.putIfAbsent(key, expireAt);
        if (old == null) {
            return true;
        }
        if (old <= System.currentTimeMillis()) {
            return lockStore.replace(key, old, expireAt);
        }
        return false;
    }

    @Override
    public void remove(String key) {
        lockStore.remove(key);
        resultStore.remove(key);
    }

    @Override
    public void saveResult(String key, Object result, long ttl, TimeUnit timeUnit) {
        long expireAt = System.currentTimeMillis() + timeUnit.toMillis(ttl);
        resultStore.put(key, new ResultWithExpiry(result, expireAt));
    }

    @Override
    public Object getResult(String key) {
        ResultWithExpiry entry = resultStore.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expiry <= System.currentTimeMillis()) {
            resultStore.remove(key);
            return null;
        }
        return entry.value;
    }

    private void evictExpired() {
        long now = System.currentTimeMillis();
        lockStore.entrySet().removeIf(e -> e.getValue() <= now);
        resultStore.entrySet().removeIf(e -> e.getValue().expiry <= now);
    }

    private record ResultWithExpiry(Object value, long expiry) {
    }
}
