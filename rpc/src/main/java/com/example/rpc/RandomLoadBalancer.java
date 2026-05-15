package com.example.rpc;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomLoadBalancer implements LoadBalancer {

    @Override
    public String select(List<String> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            throw new RuntimeException("no available address found");
        }
        return addresses.get(ThreadLocalRandom.current().nextInt(addresses.size()));
    }
}
