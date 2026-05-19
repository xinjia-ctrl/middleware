package com.example.rpc;

import com.example.rpc.client.RpcClientProxy;
import com.example.rpc.config.RpcBootstrap;
import com.example.rpc.config.RpcClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcDemo {

    private static final Logger log = LoggerFactory.getLogger(RpcDemo.class);

    public static void main(String[] args) throws Exception {
        String zkAddr = args.length > 0 ? args[0] : "127.0.0.1:2181";
        String strategy = args.length > 1 ? args[1] : "random";

        RpcClientConfig config = RpcBootstrap.newClientConfig(zkAddr);
        config.setLoadBalance(strategy);
        config.setRetryCount(2);
        config.setTimeoutSeconds(3);

        RpcClientProxy proxy = RpcBootstrap.createClientProxy(config);

        UserService userService = proxy.create(UserService.class);
        log.info(String.valueOf(userService.getUserByUserId(1)));
        log.info(String.valueOf(userService.findByName("张三")));

        OrderService orderService = proxy.create(OrderService.class);
        log.info(String.valueOf(orderService.getOrderCount(5)));

        proxy.close();
    }
}
