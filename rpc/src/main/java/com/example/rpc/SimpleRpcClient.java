package com.example.rpc;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SimpleRpcClient implements RpcClient {

    @Override
    public RpcResponse sendRequest(RpcRequest request, String host, int port) {
        try (Socket socket = new Socket(host, port);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            oos.writeObject(request);
            oos.flush();
            return (RpcResponse) ois.readObject();

        } catch (Exception e) {
            throw new RuntimeException("rpc call failed", e);
        }
    }

    @Override
    public void close() {
    }
}
