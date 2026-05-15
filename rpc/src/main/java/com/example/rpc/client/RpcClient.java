package com.example.rpc.client;
import com.example.rpc.protocol.RpcRequest;
import com.example.rpc.protocol.RpcResponse;

public interface RpcClient {

    RpcResponse sendRequest(RpcRequest request, String host, int port);

    void close();
}
