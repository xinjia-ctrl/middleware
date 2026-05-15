package com.example.rpc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class RpcClientProxy {

    private final RpcClient rpcClient;
    private final String host;
    private final int port;
    private final ServiceDiscovery discovery;

    public RpcClientProxy(RpcClient rpcClient, String host, int port) {
        this.rpcClient = rpcClient;
        this.host = host;
        this.port = port;
        this.discovery = null;
    }

    public RpcClientProxy(RpcClient rpcClient, ServiceDiscovery discovery) {
        this.rpcClient = rpcClient;
        this.host = null;
        this.port = 0;
        this.discovery = discovery;
    }

    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> interfaceClass) {
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new RpcInvocationHandler());
    }

    private class RpcInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }

            RpcRequest request = new RpcRequest(
                    method.getDeclaringClass().getName(),
                    method.getName(),
                    method.getParameterTypes(),
                    args);

            if (discovery != null) {
                String address = discovery.discover(method.getDeclaringClass().getName());
                String[] parts = address.split(":");
                RpcResponse response = rpcClient.sendRequest(request, parts[0], Integer.parseInt(parts[1]));
                if (!response.isSuccess()) {
                    throw new RuntimeException(response.getMessage());
                }
                return response.getData();
            } else {
                RpcResponse response = rpcClient.sendRequest(request, host, port);
                if (!response.isSuccess()) {
                    throw new RuntimeException(response.getMessage());
                }
                return response.getData();
            }
        }
    }
}
