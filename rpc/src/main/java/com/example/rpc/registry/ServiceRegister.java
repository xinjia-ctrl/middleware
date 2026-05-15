package com.example.rpc.registry;

import java.util.List;

public interface ServiceRegister {

    void register(String serviceName, String address);

    List<String> discover(String serviceName);

    void close();
}
