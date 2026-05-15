package com.example.rpc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

public class RpcClientProxy {

    private final RpcClient rpcClient;
    private final ServiceDiscovery discovery;
    private final String host;
    private final int port;
    private final int retryCount;

    private static final Set<Integer> RETRYABLE_CODES = new HashSet<>();

    static {
        RETRYABLE_CODES.add(429); // rate limited
        RETRYABLE_CODES.add(503); // circuit breaker open
    }

    public RpcClientProxy(RpcClient rpcClient, String host, int port) {
        this(rpcClient, host, port, 2);
    }

    public RpcClientProxy(RpcClient rpcClient, String host, int port, int retryCount) {
        this.rpcClient = rpcClient;
        this.discovery = null;
        this.host = host;
        this.port = port;
        this.retryCount = retryCount;
    }

    public RpcClientProxy(RpcClient rpcClient, ServiceDiscovery discovery) {
        this(rpcClient, discovery, 2);
    }

    public RpcClientProxy(RpcClient rpcClient, ServiceDiscovery discovery, int retryCount) {
        this.rpcClient = rpcClient;
        this.discovery = discovery;
        this.host = null;
        this.port = 0;
        this.retryCount = retryCount;
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

            RuntimeException lastException = null;

            for (int i = 0; i <= retryCount; i++) {
                boolean isRetry = i > 0;
                try {
                    String targetHost;
                    int targetPort;
                    if (discovery != null) {
                        String address = discovery.discover(interfaceName, isRetry);
                        String[] parts = address.split(":");
                        targetHost = parts[0];
                        targetPort = Integer.parseInt(parts[1]);
                    } else {
                        targetHost = host;
                        targetPort = port;
                    }

                    RpcResponse response = rpcClient.sendRequest(request, targetHost, targetPort);
                    if (response.isSuccess()) {
                        return response.getData();
                    }
                    if (!isRetryable(response.getCode())) {
                        throw new RuntimeException(response.getMessage());
                    }
                    lastException = new RuntimeException(response.getMessage());
                } catch (Exception e) {
                    if (i >= retryCount) {
                        throw e;
                    }
                    lastException = e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
                }
            }

            throw lastException;
        }

        private boolean isRetryable(int code) {
            return RETRYABLE_CODES.contains(code);
        }
    }
}
