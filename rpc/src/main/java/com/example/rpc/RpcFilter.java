package com.example.rpc;

public interface RpcFilter {

    boolean before(RpcRequest request, RpcResponse response);

    default void after(RpcRequest request, RpcResponse response) {
    }
}
