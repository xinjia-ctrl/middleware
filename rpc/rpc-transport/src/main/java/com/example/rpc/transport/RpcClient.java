package com.example.rpc.transport;

/**
 * RPC 客户端接口，负责连接和发送请求
 */
public interface RpcClient extends AutoCloseable {

    void connect(String host, int port);

    boolean isConnected();

    @Override
    void close();
}
