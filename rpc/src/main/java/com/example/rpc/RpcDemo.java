package com.example.rpc;

public class RpcDemo {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;

        // 一行切换通信方式 —— 注释/取消注释即可
        RpcClient transport = new NettyRpcClient();
        // RpcClient transport = new SimpleRpcClient();

        RpcClientProxy proxy = new RpcClientProxy(transport, "127.0.0.1", port);

        UserService userService = proxy.create(UserService.class);
        System.out.println(userService.getUserByUserId(1));
        System.out.println(userService.findByName("张三"));

        OrderService orderService = proxy.create(OrderService.class);
        System.out.println(orderService.getOrderCount(5));

        transport.close();
    }
}
