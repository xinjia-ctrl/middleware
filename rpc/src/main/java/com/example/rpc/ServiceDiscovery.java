package com.example.rpc;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceDiscovery {

    private final ServiceRegister register;
    private final LoadBalancer loadBalancer;
    private final Map<String, List<String>> cache = new ConcurrentHashMap<>();

    public ServiceDiscovery(String zkAddr) {
        this.register = new ZkServiceRegister(zkAddr);
        this.loadBalancer = new RandomLoadBalancer();
    }

    public ServiceDiscovery(String zkAddr, LoadBalancer loadBalancer) {
        this.register = new ZkServiceRegister(zkAddr);
        this.loadBalancer = loadBalancer;
    }

    public String discover(String serviceName) {
        List<String> addresses = cache.computeIfAbsent(serviceName, register::discover);
        if (addresses.isEmpty()) {
            throw new RuntimeException("no providers for service: " + serviceName);
        }
        return loadBalancer.select(addresses);
    }

    public void close() {
        register.close();
    }
}
