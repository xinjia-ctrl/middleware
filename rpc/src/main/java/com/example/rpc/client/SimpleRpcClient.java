package com.example.rpc.client;

import com.example.rpc.config.RpcConstants;
import com.example.rpc.protocol.MessageType;
import com.example.rpc.protocol.ObjectSerializer;
import com.example.rpc.protocol.RpcRequest;
import com.example.rpc.protocol.RpcResponse;
import com.example.rpc.protocol.JsonSerializer;
import com.example.rpc.protocol.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.zip.GZIPInputStream;

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

            // 协议: [魔数2B][版本2B][消息类型2B][序列化方式2B][压缩标记1B][保留1B][body长度4B][数据]
            dos.writeShort(RpcConstants.MAGIC_NUMBER);
            dos.writeShort(RpcConstants.PROTOCOL_VERSION);
            dos.writeShort(MessageType.REQUEST);
            dos.writeShort(serializer.getType());
            dos.writeByte(0);   // 压缩标记
            dos.writeByte(0);   // 保留
            dos.writeInt(body.length);
            dos.write(body);
            dos.flush();

            // 读响应头 (14B)
            short magic = dis.readShort();
            if (magic != RpcConstants.MAGIC_NUMBER) {
                throw new RuntimeException("invalid magic number: " + magic);
            }
            dis.readShort();                    // 版本
            dis.readShort();                    // 消息类型
            short serType = dis.readShort();    // 序列化方式
            byte compressFlag = dis.readByte(); // 压缩标记
            dis.readByte();                     // 保留
            int len = dis.readInt();            // 消息体长度

            byte[] respBody = new byte[len];
            dis.readFully(respBody);

            if (compressFlag == RpcConstants.GZIP_COMPRESS) {
                ByteArrayInputStream bis = new ByteArrayInputStream(respBody);
                try (GZIPInputStream gzip = new GZIPInputStream(bis);
                     ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = gzip.read(buf)) != -1) {
                        bos.write(buf, 0, n);
                    }
                    respBody = bos.toByteArray();
                }
            }

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
