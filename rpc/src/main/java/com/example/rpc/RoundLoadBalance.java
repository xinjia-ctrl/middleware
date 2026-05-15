package com.example.rpc;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundLoadBalance implements LoadBalance {

    private final AtomicInteger index = new AtomicInteger(0);

    @Override
    public String select(List<String> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            throw new RuntimeException("no available address found");
        }
        return addresses.get(index.getAndIncrement() % addresses.size());
    }
}
