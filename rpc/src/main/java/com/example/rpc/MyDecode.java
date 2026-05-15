package com.example.rpc;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        if (in.readableBytes() < 8) {
            return;
        }

        in.markReaderIndex();

        short messageType = in.readShort();
        short serializerType = in.readShort();
        int bodyLength = in.readInt();

        if (in.readableBytes() < bodyLength) {
            in.resetReaderIndex();
            return;
        }

        byte[] body = new byte[bodyLength];
        in.readBytes(body);

        Serializer serializer = serializers.get(serializerType);
        if (serializer == null) {
            throw new RuntimeException("unknown serializer type: " + serializerType);
        }

        if (messageType == MessageType.HEARTBEAT) {
            out.add(RpcHeartbeat.INSTANCE);
            return;
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
