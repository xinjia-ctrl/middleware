package com.example.rpc;

import java.util.List;

public interface ServiceRegister {

    void register(String serviceName, String address);

    List<String> discover(String serviceName);

    void close();
}
