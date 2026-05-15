package com.example.rpc.protocol;
import com.example.rpc.config.RpcConstants;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class MyDecode extends ByteToMessageDecoder {

    private static final Map<Short, Serializer> serializers = new HashMap<>();

    static {
        ObjectSerializer objectSerializer = new ObjectSerializer();
        serializers.put(objectSerializer.getType(), objectSerializer);
        JsonSerializer jsonSerializer = new JsonSerializer();
        serializers.put(jsonSerializer.getType(), jsonSerializer);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < RpcConstants.HEADER_SIZE) {
            return;
        }

        in.markReaderIndex();

        short magic = in.readShort();
        if (magic != RpcConstants.MAGIC_NUMBER) {
            ctx.close();
            return;
        }

        short version = in.readShort();
        if (version > RpcConstants.PROTOCOL_VERSION) {
            ctx.close();
            return;
        }

        short messageType = in.readShort();
        short serializerType = in.readShort();
        byte compressFlag = in.readByte();
        in.readByte(); // 保留
        int bodyLength = in.readInt();

        if (in.readableBytes() < bodyLength) {
            in.resetReaderIndex();
            return;
        }

        byte[] body = new byte[bodyLength];
        in.readBytes(body);

        if (compressFlag == RpcConstants.GZIP_COMPRESS) {
            ByteArrayInputStream bis = new ByteArrayInputStream(body);
            try (GZIPInputStream gzip = new GZIPInputStream(bis);
                 java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = gzip.read(buf)) != -1) {
                    bos.write(buf, 0, n);
                }
                body = bos.toByteArray();
            }
        }

        if (messageType == MessageType.HEARTBEAT) {
            out.add(RpcHeartbeat.INSTANCE);
            return;
        }

        Serializer serializer = serializers.get(serializerType);
        if (serializer == null) {
            throw new RuntimeException("unknown serializer type: " + serializerType);
        }

        Class<?> targetClass;
        if (messageType == MessageType.REQUEST) {
            targetClass = RpcRequest.class;
        } else if (messageType == MessageType.RESPONSE) {
            targetClass = RpcResponse.class;
        } else {
            throw new RuntimeException("unknown message type: " + messageType);
        }

        Object obj = serializer.deserialize(body, targetClass);
        out.add(obj);
    }
}
