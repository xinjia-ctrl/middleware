package com.example.rpc.transport;

/**
 * RPC 服务端接口，负责启动和关闭
 */
public interface RpcServer {

    void start(int port);

    void shutdown();
}
