package com.example.rpc.registry.local;

import com.example.rpc.registry.RegistryService;
import com.example.rpc.registry.ServiceMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 本地注册中心（开发/测试用）
 */
public class LocalRegistryService implements RegistryService {

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<ServiceMeta>> registry = new ConcurrentHashMap<>();

    @Override
    public void register(ServiceMeta serviceMeta) {
        registry.computeIfAbsent(serviceMeta.getServiceName(), k -> new CopyOnWriteArrayList<>())
                .addIfAbsent(serviceMeta);
    }

    @Override
    public void unregister(ServiceMeta serviceMeta) {
        registry.computeIfPresent(serviceMeta.getServiceName(), (k, list) -> {
            list.remove(serviceMeta);
            return list.isEmpty() ? null : list;
        });
    }

    @Override
    public List<ServiceMeta> discover(String serviceName) {
        return new ArrayList<>(registry.getOrDefault(serviceName, new CopyOnWriteArrayList<>()));
    }

    @Override
    public void close() {
        registry.clear();
    }
}
