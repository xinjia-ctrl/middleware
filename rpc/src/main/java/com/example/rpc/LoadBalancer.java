package com.example.rpc;

import java.util.List;

public interface LoadBalancer {

    String select(List<String> addresses);
}
