package com.example.rpc;

import java.util.List;

public class RpcServer {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        String zkAddr = args.length > 1 ? args[1] : "127.0.0.1:2181";
        String serverAddress = "127.0.0.1:" + port;

        ServiceProvider provider = RpcBootstrap.newServiceProvider(zkAddr, serverAddress);
        provider.addService(UserService.class, new UserServiceImpl());
        provider.addService(OrderService.class, new OrderServiceImpl());

        List<RpcFilter> filters = List.of(
                new RpcRateLimitFilter(),
                new RpcCircuitBreakerFilter(),
                new RpcIdempotentFilter()
        );

        NettyRpcServer server = RpcBootstrap.createServer(provider, new ObjectSerializer(), filters);
        server.start(port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop();
            provider.close();
        }));
    }
}
