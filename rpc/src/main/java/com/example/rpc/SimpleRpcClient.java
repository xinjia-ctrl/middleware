package com.example.rpc;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class SimpleRpcClient implements RpcClient {

    private final Serializer serializer;

    public SimpleRpcClient() {
        this(new ObjectSerializer());
    }

    public SimpleRpcClient(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public RpcResponse sendRequest(RpcRequest request, String host, int port) {
        try (Socket socket = new Socket(host, port);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            byte[] body = serializer.serialize(request);
            // 自定义协议: [消息类型2B][序列化方式2B][消息体长度4B][数据]
            dos.writeShort(MessageType.REQUEST);
            dos.writeShort(serializer.getType());
            dos.writeInt(body.length);
            dos.write(body);
            dos.flush();

            // 读响应头
            dis.readShort();                    // 消息类型
            short serType = dis.readShort();    // 序列化方式
            int len = dis.readInt();            // 消息体长度
            byte[] respBody = new byte[len];
            dis.readFully(respBody);            // 数据

            Serializer deserializer = serType == RpcConstants.SERIALIZER_JDK
                    ? new ObjectSerializer() : new JsonSerializer();
            return deserializer.deserialize(respBody, RpcResponse.class);

        } catch (Exception e) {
            throw new RuntimeException("rpc call failed", e);
        }
    }

    @Override
    public void close() {
    }
}
