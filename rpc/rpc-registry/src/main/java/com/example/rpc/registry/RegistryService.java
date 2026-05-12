package com.example.rpc.registry;

import java.util.List;

/**
 * 注册中心接口
 */
public interface RegistryService {

    void register(ServiceMeta serviceMeta);

    void unregister(ServiceMeta serviceMeta);

    List<ServiceMeta> discover(String serviceName);

    void close();
}
