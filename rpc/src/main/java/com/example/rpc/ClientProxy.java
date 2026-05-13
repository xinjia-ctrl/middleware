package com.example.rpc;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.Socket;

public class ClientProxy {

    private final String host;
    private final int port;

    public ClientProxy(String host, int port) {
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

            try (Socket socket = new Socket(host, port)) {
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                oos.writeObject(request);
                oos.flush();

                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                RpcResponse response = (RpcResponse) ois.readObject();

                if (!response.isSuccess()) {
                    throw new RuntimeException(response.getMessage());
                }
                return response.getData();
            }
        }
    }
}
