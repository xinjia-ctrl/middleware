package com.example.rpc.client.loadbalance;

import com.example.rpc.registry.ServiceMeta;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询负载均衡
 */
public class RoundRobinLoadBalance implements LoadBalance {

    private final AtomicInteger index = new AtomicInteger(0);

    @Override
    public ServiceMeta select(List<ServiceMeta> services) {
        if (services.isEmpty()) {
            throw new IllegalArgumentException("no services available");
        }
        return services.get(index.getAndIncrement() % services.size());
    }
}
