package com.example.rpc.filter;
import com.example.rpc.protocol.RpcRequest;
import com.example.rpc.protocol.RpcResponse;

public interface RpcFilter {

    boolean before(RpcRequest request, RpcResponse response);

    default void after(RpcRequest request, RpcResponse response) {
    }
}
