package com.example.rpc.client.loadbalance;

import com.example.rpc.registry.ServiceMeta;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机负载均衡
 */
public class RandomLoadBalance implements LoadBalance {

    @Override
    public ServiceMeta select(List<ServiceMeta> services) {
        if (services.isEmpty()) {
            throw new IllegalArgumentException("no services available");
        }
        return services.get(ThreadLocalRandom.current().nextInt(services.size()));
    }
}
