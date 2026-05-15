package com.example.rpc;

import com.example.rpc.client.NettyRpcClient;
import com.example.rpc.client.RpcClient;
import com.example.rpc.client.RpcClientProxy;
import com.example.rpc.config.RpcBootstrap;
import com.example.rpc.config.RpcClientConfig;
import com.example.rpc.registry.ServiceDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcDemo {

    private static final Logger log = LoggerFactory.getLogger(RpcDemo.class);

    public static void main(String[] args) throws Exception {
        String zkAddr = args.length > 0 ? args[0] : "127.0.0.1:2181";
        String strategy = args.length > 1 ? args[1] : "random";

        RpcClientConfig config = RpcBootstrap.newClientConfig(zkAddr);
        config.setLoadBalance(strategy);

        ServiceDiscovery discovery = new ServiceDiscovery(zkAddr, strategy);
        RpcClient transport = new NettyRpcClient(config.getSerializer());
        RpcClientProxy proxy = new RpcClientProxy(transport, discovery);

        UserService userService = proxy.create(UserService.class);
        log.info(String.valueOf(userService.getUserByUserId(1)));
        log.info(String.valueOf(userService.findByName("张三")));

        OrderService orderService = proxy.create(OrderService.class);
        log.info(String.valueOf(orderService.getOrderCount(5)));

        transport.close();
        discovery.close();
    }
}
