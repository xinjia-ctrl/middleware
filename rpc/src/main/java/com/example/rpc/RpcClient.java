package com.example.rpc;

public class RpcClient {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;

        ClientProxy proxy = new ClientProxy("127.0.0.1", port);
        UserService userService = proxy.create(UserService.class);

        User user = userService.getUserByUserId(1);
        System.out.println("Client received: " + user);
    }
}
