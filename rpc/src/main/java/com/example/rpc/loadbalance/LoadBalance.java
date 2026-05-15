package com.example.rpc.loadbalance;

import java.util.List;

public interface LoadBalance {

    String select(List<String> addresses);

    default String select(List<String> addresses, String key) {
        return select(addresses);
    }
}
