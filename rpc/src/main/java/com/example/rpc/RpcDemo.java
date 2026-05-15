package com.example.rpc;

public class RpcDemo {

    public static void main(String[] args) throws Exception {
        String zkAddr = args.length > 0 ? args[0] : "127.0.0.1:2181";

        ServiceDiscovery discovery = new ServiceDiscovery(zkAddr);

        RpcClient transport = new NettyRpcClient();
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
