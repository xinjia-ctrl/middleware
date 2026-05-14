package com.example.rpc;

public interface RpcClient {

    RpcResponse sendRequest(RpcRequest request, String host, int port);

    void close();
}
