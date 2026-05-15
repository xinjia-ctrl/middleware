package com.example.rpc.server;
import com.example.rpc.OrderService;
import com.example.rpc.OrderServiceImpl;
import com.example.rpc.UserService;
import com.example.rpc.UserServiceImpl;
import com.example.rpc.config.RpcBootstrap;
import com.example.rpc.filter.RpcCircuitBreakerFilter;
import com.example.rpc.filter.RpcFilter;
import com.example.rpc.filter.RpcIdempotentFilter;
import com.example.rpc.filter.RpcRateLimitFilter;
import com.example.rpc.protocol.ObjectSerializer;
import com.example.rpc.server.ServiceProvider;

import com.example.circuitbreaker.strategy.SlidingWindowCircuitBreaker;

import java.util.List;
import java.util.concurrent.TimeUnit;

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
                new RpcCircuitBreakerFilter(new SlidingWindowCircuitBreaker(), 10, 0.5, 3, 10, TimeUnit.SECONDS),
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
