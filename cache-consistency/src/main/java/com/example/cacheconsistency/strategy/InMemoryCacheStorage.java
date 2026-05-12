package com.example.cacheconsistency.strategy;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class InMemoryCacheStorage implements CacheStorage {

    private final ConcurrentHashMap<String, ValueWithExpiry> store = new ConcurrentHashMap<>();

    @Override
    public void delete(String key) {
        store.remove(key);
    }

    @Override
    public void set(String key, String value, long ttl, TimeUnit unit) {
        long expiry = System.currentTimeMillis() + unit.toMillis(ttl);
        store.put(key, new ValueWithExpiry(value, expiry));
        evictExpired();
    }

    private void evictExpired() {
        long now = System.currentTimeMillis();
        store.forEach((key, v) -> {
            if (v.expiry <= now) {
                store.remove(key, v);
            }
        });
    }

    // visible for testing
    int size() {
        evictExpired();
        return store.size();
    }

    private record ValueWithExpiry(String value, long expiry) {}

    // visible for testing
    String get(String key) {
        ValueWithExpiry v = store.get(key);
        if (v == null) return null;
        if (v.expiry <= System.currentTimeMillis()) {
            store.remove(key, v);
            return null;
        }
        return v.value;
    }
}
