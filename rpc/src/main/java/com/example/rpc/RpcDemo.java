package com.example.rpc;

public class RpcDemo {

    public static void main(String[] args) throws Exception {
        String zkAddr = args.length > 0 ? args[0] : "127.0.0.1:2181";
        String strategy = args.length > 1 ? args[1] : "random";

        // 使用配置类组装客户端
        RpcClientConfig config = RpcBootstrap.newClientConfig(zkAddr);
        config.setLoadBalance(strategy);

        ServiceDiscovery discovery = new ServiceDiscovery(zkAddr, strategy);
        RpcClient transport = new NettyRpcClient(config.getSerializer());
        RpcClientProxy proxy = new RpcClientProxy(transport, discovery);

        UserService userService = proxy.create(UserService.class);
        System.out.println(userService.getUserByUserId(1));
        System.out.println(userService.findByName("张三"));

        OrderService orderService = proxy.create(OrderService.class);
        System.out.println(orderService.getOrderCount(5));

        transport.close();
        discovery.close();
    }
}
