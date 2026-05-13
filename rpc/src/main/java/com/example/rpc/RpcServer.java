package com.example.rpc;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class RpcServer {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        UserService userService = new UserServiceImpl();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("RPC Server started on port " + port);

            while (true) {
                try (Socket socket = serverSocket.accept()) {
                    ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                    Integer userId = (Integer) ois.readObject();
                    System.out.println("Server received userId: " + userId);

                    User user = userService.getUserByUserId(userId);

                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                    oos.writeObject(user);
                    oos.flush();
                    System.out.println("Server sent: " + user);
                }
            }
        }
    }
}
