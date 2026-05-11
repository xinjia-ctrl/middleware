package com.example.idempotent.strategy;

import java.util.concurrent.TimeUnit;

public interface IdempotentStorage {

    /**
     * 尝试存储幂等 key，key 已存在时返回 false
     */
    boolean trySave(String key, long ttl, TimeUnit timeUnit);

    /**
     * 删除幂等 key（同时清理关联的缓存结果）
     */
    void remove(String key);

    /**
     * 缓存接口执行结果，后续重复请求可直接返回
     */
    default void saveResult(String key, Object result, long ttl, TimeUnit timeUnit) {
    }

    /**
     * 获取缓存的执行结果
     */
    default Object getResult(String key) {
        return null;
    }
}
