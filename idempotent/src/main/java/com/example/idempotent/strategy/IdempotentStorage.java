package com.example.idempotent.strategy;

import java.util.concurrent.TimeUnit;

public interface IdempotentStorage {

    /**
     * 尝试存储幂等 key，key 已存在时返回 false
     */
    boolean trySave(String key, long ttl, TimeUnit timeUnit);

    /**
     * 删除幂等 key
     */
    void remove(String key);
}
