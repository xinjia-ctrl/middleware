package com.example.rpc.client;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface RpcFallback {

    Object fallback(String interfaceName, Method method, Object[] args, Throwable cause);

    default void onSuccess(String interfaceName, Method method, Object result) {
    }

    static RpcFallback cache() {
        return new CacheFallback();
    }

    static RpcFallback mock() {
        return (name, method, args, cause) -> null;
    }

    static RpcFallback chain(RpcFallback first, RpcFallback second) {
        return new RpcFallback() {
            @Override
            public Object fallback(String name, Method method, Object[] args, Throwable cause) {
                Object result = first.fallback(name, method, args, cause);
                if (result != null) {
                    return result;
                }
                return second.fallback(name, method, args, cause);
            }

            @Override
            public void onSuccess(String interfaceName, Method method, Object result) {
                first.onSuccess(interfaceName, method, result);
                second.onSuccess(interfaceName, method, result);
            }
        };
    }
}

class CacheFallback implements RpcFallback {

    private final Map<String, Object> cache = new ConcurrentHashMap<>();

    @Override
    public Object fallback(String interfaceName, Method method, Object[] args, Throwable cause) {
        String key = interfaceName + "#" + method.getName();
        return cache.get(key);
    }

    @Override
    public void onSuccess(String interfaceName, Method method, Object result) {
        String key = interfaceName + "#" + method.getName();
        cache.put(key, result);
    }
}
