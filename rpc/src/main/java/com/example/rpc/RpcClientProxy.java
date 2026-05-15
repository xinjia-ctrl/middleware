package com.example.rpc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class RpcClientProxy {

    private final RpcClient rpcClient;
    private final ServiceDiscovery discovery;
    private final String host;
    private final int port;

    public RpcClientProxy(RpcClient rpcClient, String host, int port) {
        this.rpcClient = rpcClient;
        this.discovery = null;
        this.host = host;
        this.port = port;
    }

    public RpcClientProxy(RpcClient rpcClient, ServiceDiscovery discovery) {
        this.rpcClient = rpcClient;
        this.discovery = discovery;
        this.host = null;
        this.port = 0;
    }

    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> interfaceClass) {
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new RpcInvocationHandler(interfaceClass.getName()));
    }

    private class RpcInvocationHandler implements InvocationHandler {

        private final String interfaceName;

        RpcInvocationHandler(String interfaceName) {
            this.interfaceName = interfaceName;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }

            RpcRequest request = new RpcRequest(
                    interfaceName,
                    method.getName(),
                    method.getParameterTypes(),
                    args);

            String targetHost;
            int targetPort;
            if (discovery != null) {
                String address = discovery.discover(interfaceName);
                String[] parts = address.split(":");
                targetHost = parts[0];
                targetPort = Integer.parseInt(parts[1]);
            } else {
                targetHost = host;
                targetPort = port;
            }

            RpcResponse response = rpcClient.sendRequest(request, targetHost, targetPort);
            if (!response.isSuccess()) {
                throw new RuntimeException(response.getMessage());
            }
            return response.getData();
        }
    }
}
