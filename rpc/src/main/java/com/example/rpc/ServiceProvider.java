package com.example.rpc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceProvider {

    private final Map<String, Object> serviceMap = new ConcurrentHashMap<>();
    private final ServiceRegister register;
    private final String serverAddress;

    public ServiceProvider(ServiceRegister register, String serverAddress) {
        this.register = register;
        this.serverAddress = serverAddress;
    }

    public void addService(Class<?> interfaceClass, Object impl) {
        String name = interfaceClass.getName();
        serviceMap.put(name, impl);
        register.register(name, serverAddress);
    }

    public Object getService(String name) {
        return serviceMap.get(name);
    }
}
