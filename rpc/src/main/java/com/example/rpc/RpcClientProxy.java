package com.example.rpc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class RpcClientProxy {

    private final RpcClient rpcClient;
    private final String host;
    private final int port;

    public RpcClientProxy(RpcClient rpcClient, String host, int port) {
        this.rpcClient = rpcClient;
        this.host = host;
        this.port = port;
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

            RpcResponse response = rpcClient.sendRequest(request, host, port);

            if (!response.isSuccess()) {
                throw new RuntimeException(response.getMessage());
            }
            return response.getData();
        }
    }
}
