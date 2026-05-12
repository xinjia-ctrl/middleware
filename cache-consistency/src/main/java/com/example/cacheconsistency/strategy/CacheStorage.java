package com.example.cacheconsistency.strategy;

import java.util.concurrent.TimeUnit;

public interface CacheStorage {

    void delete(String key);

    void set(String key, String value, long ttl, TimeUnit unit);
}
