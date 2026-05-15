package com.example.rpc;

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
        return createServer(provider, new ObjectSerializer());
    }

    public static NettyRpcServer createServer(ServiceProvider provider, Serializer serializer) {
        return new NettyRpcServer(provider, serializer);
    }
}
