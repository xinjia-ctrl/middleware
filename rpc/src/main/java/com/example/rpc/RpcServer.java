package com.example.rpc;

public class RpcServer {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;

        NettyRpcServer server = new NettyRpcServer();
        server.register(UserService.class, new UserServiceImpl());
        server.register(OrderService.class, new OrderServiceImpl());
        server.start(port);

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }
}
