package com.example.rpc.server;

import com.example.rpc.core.serialization.JsonSerializer;
import com.example.rpc.core.serialization.Serializer;
import com.example.rpc.registry.RegistryService;
import com.example.rpc.registry.ServiceMeta;
import com.example.rpc.transport.netty.NettyRpcServer;
import com.example.rpc.transport.netty.RpcRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RPC 服务端核心：注册服务、启动监听、处理请求
 */
public class RpcServiceProvider {

    private static final Logger log = LoggerFactory.getLogger(RpcServiceProvider.class);

    private final Map<String, Object> serviceBeanMap = new ConcurrentHashMap<>();
    private final RegistryService registryService;
    private final Serializer serializer;
    private final String host;
    private final int port;
    private NettyRpcServer server;

    public RpcServiceProvider(RegistryService registryService, String host, int port) {
        this(registryService, new JsonSerializer(), host, port);
    }

    public RpcServiceProvider(RegistryService registryService, Serializer serializer, String host, int port) {
        this.registryService = registryService;
        this.serializer = serializer;
        this.host = host;
        this.port = port;
    }

    /**
     * 注册服务实现类
     */
    public void registerService(Class<?> serviceInterface, Object serviceImpl) {
        String serviceName = serviceInterface.getName();
        serviceBeanMap.put(serviceName, serviceImpl);

        ServiceMeta meta = new ServiceMeta(serviceName, host, port);
        registryService.register(meta);
        log.info("register service: {} -> {}:{}", serviceName, host, port);
    }

    /**
     * 启动 RPC 服务端
     */
    public void start() {
        server = new NettyRpcServer(serializer, new RpcRequestHandler(serializer, serviceBeanMap));
        server.start(port);
    }

    /**
     * 停止 RPC 服务端
     */
    public void stop() {
        if (server != null) {
            server.shutdown();
        }
        registryService.close();
    }

    public Map<String, Object> getServiceBeanMap() {
        return serviceBeanMap;
    }
}
