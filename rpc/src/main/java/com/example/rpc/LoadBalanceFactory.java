package com.example.rpc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LoadBalanceFactory {

    private static final Map<String, LoadBalance> strategies = new ConcurrentHashMap<>();

    static {
        strategies.put("random", new RandomLoadBalance());
        strategies.put("round", new RoundLoadBalance());
        strategies.put("consistentHash", new ConsistentHashLoadBalance());
    }

    public static LoadBalance get(String name) {
        LoadBalance balance = strategies.get(name);
        if (balance == null) {
            throw new RuntimeException("unknown load balance strategy: " + name);
        }
        return balance;
    }

    public static void register(String name, LoadBalance balance) {
        strategies.put(name, balance);
    }
}
