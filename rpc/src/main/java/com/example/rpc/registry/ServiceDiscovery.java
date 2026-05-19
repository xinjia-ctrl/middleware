package com.example.rpc.registry;
import com.example.rpc.loadbalance.LoadBalance;
import com.example.rpc.loadbalance.LoadBalanceFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceDiscovery {

    private final ServiceRegister register;
    private final LoadBalance loadBalance;
    private final Map<String, List<String>> cache = new ConcurrentHashMap<>();

    public ServiceDiscovery(String zkAddr) {
        this(zkAddr, "random");
    }

    public ServiceDiscovery(String zkAddr, String strategy) {
        this.register = new ZkServiceRegister(zkAddr);
        this.loadBalance = LoadBalanceFactory.get(strategy);
    }

    public ServiceDiscovery(String zkAddr, LoadBalance loadBalance) {
        this.register = new ZkServiceRegister(zkAddr);
        this.loadBalance = loadBalance;
    }

    public String discover(String serviceName) {
        return discover(serviceName, false);
    }

    public String discover(String serviceName, boolean forceRefresh) {
        List<String> addresses = getAddresses(serviceName, forceRefresh);
        return loadBalance.select(addresses);
    }

    public String discover(String serviceName, String key) {
        List<String> addresses = getAddresses(serviceName, false);
        return loadBalance.select(addresses, key);
    }

    public String discover(String serviceName, String methodName, Object[] args, boolean forceRefresh) {
        List<String> addresses = getAddresses(serviceName, forceRefresh);
        String key = serviceName + "#" + methodName + "#" + Arrays.deepHashCode(args == null ? new Object[0] : args);
        return loadBalance.select(addresses, key);
    }

    private List<String> getAddresses(String serviceName, boolean forceRefresh) {
        if (forceRefresh) {
            cache.remove(serviceName);
        }
        List<String> addresses = cache.computeIfAbsent(serviceName, register::discover);
        if (addresses.isEmpty()) {
            throw new RuntimeException("no providers for service: " + serviceName);
        }
        return addresses;
    }

    public void close() {
        register.close();
    }
}
