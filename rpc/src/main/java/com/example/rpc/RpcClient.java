package com.example.rpc;

public class RpcClient {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;

        ClientProxy proxy = new ClientProxy("127.0.0.1", port);

        // 调用 UserService 的两个方法
        UserService userService = proxy.create(UserService.class);

        User user1 = userService.getUserByUserId(1);
        System.out.println("getUserByUserId(1):  " + user1);

        User user2 = userService.findByName("张三");
        System.out.println("findByName(张三):     " + user2);

        // 调用另一个服务 OrderService
        OrderService orderService = proxy.create(OrderService.class);

        Integer count = orderService.getOrderCount(5);
        System.out.println("getOrderCount(5):     " + count);
    }
}
