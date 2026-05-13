package com.example.rpc;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RpcServer {

    private final Map<String, Object> serviceMap = new ConcurrentHashMap<>();

    public void register(Class<?> interfaceClass, Object serviceImpl) {
        serviceMap.put(interfaceClass.getName(), serviceImpl);
        System.out.println("register service: " + interfaceClass.getName());
    }

    public void start(int port) throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("RPC Server started on port " + port);
            while (true) {
                try (Socket socket = serverSocket.accept()) {
                    ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                    RpcRequest request = (RpcRequest) ois.readObject();
                    System.out.println("Received: " + request);

                    RpcResponse response;
                    Object serviceImpl = serviceMap.get(request.getInterfaceName());
                    if (serviceImpl == null) {
                        response = new RpcResponse(500, null,
                                "service not found: " + request.getInterfaceName());
                    } else {
                        try {
                            Method method = serviceImpl.getClass().getMethod(
                                    request.getMethodName(), request.getParameterTypes());
                            Object result = method.invoke(serviceImpl, request.getParameters());
                            response = new RpcResponse(200, result, null);
                        } catch (Exception e) {
                            response = new RpcResponse(500, null, e.getCause().getMessage());
                        }
                    }

                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                    oos.writeObject(response);
                    oos.flush();
                    System.out.println("Sent: " + response);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        RpcServer server = new RpcServer();
        server.register(UserService.class, new UserServiceImpl());
        server.register(OrderService.class, new OrderServiceImpl());
        server.start(port);
    }
}
