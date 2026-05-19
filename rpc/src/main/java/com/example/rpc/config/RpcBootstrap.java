package com.example.rpc.config;
import com.example.rpc.client.NettyRpcClient;
import com.example.rpc.client.RpcClient;
import com.example.rpc.client.RpcClientProxy;
import com.example.rpc.client.RpcFallback;
import com.example.rpc.filter.RpcFilter;
import com.example.rpc.server.ServiceProvider;
import com.example.rpc.server.NettyRpcServer;
import com.example.rpc.protocol.Serializer;
import com.example.rpc.protocol.ObjectSerializer;
import com.example.rpc.registry.ServiceDiscovery;
import com.example.rpc.registry.ServiceRegister;
import com.example.rpc.registry.ZkServiceRegister;

import java.util.List;

public class RpcBootstrap {

    public static RpcClientConfig newClientConfig() {
        return new RpcClientConfig();
    }

    public static RpcClientConfig newClientConfig(String zkAddr) {
        return new RpcClientConfig(zkAddr);
    }

    public static ServiceProvider newServiceProvider(String zkAddr, String serverAddress) {
        ServiceRegister register = new ZkServiceRegister(zkAddr);
        return new ServiceProvider(register, serverAddress);
    }

    public static NettyRpcServer createServer(ServiceProvider provider) {
        return createServer(provider, new ObjectSerializer(), List.of());
    }

    public static NettyRpcServer createServer(ServiceProvider provider, Serializer serializer) {
        return createServer(provider, serializer, List.of());
    }

    public static NettyRpcServer createServer(ServiceProvider provider, Serializer serializer, List<RpcFilter> filters) {
        return new NettyRpcServer(provider, serializer, filters);
    }

    public static RpcClient createClient(RpcClientConfig config) {
        return new NettyRpcClient(config.getSerializer(), config.getTimeoutSeconds());
    }

    public static RpcClientProxy createClientProxy(RpcClientConfig config) {
        return createClientProxy(config, null);
    }

    public static RpcClientProxy createClientProxy(RpcClientConfig config, RpcFallback fallback) {
        ServiceDiscovery discovery = new ServiceDiscovery(config.getZkAddr(), config.getLoadBalance());
        RpcClient client = createClient(config);
        return new RpcClientProxy(client, discovery, config.getRetryCount(), fallback);
    }
}
