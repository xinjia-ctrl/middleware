package com.example.rpc;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MyEncode extends MessageToByteEncoder<Object> {

    private final Serializer serializer;

    public MyEncode(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        if (msg instanceof RpcHeartbeat) {
            out.writeShort(MessageType.HEARTBEAT);
            out.writeShort(0);
            out.writeInt(0);
            return;
        }

        short messageType;
        if (msg instanceof RpcRequest) {
            messageType = MessageType.REQUEST;
        } else if (msg instanceof RpcResponse) {
            messageType = MessageType.RESPONSE;
        } else {
            throw new RuntimeException("unsupported message type: " + msg.getClass());
        }

        byte[] body = serializer.serialize(msg);

        out.writeShort(messageType);
        out.writeShort(serializer.getType());
        out.writeInt(body.length);
        out.writeBytes(body);
    }
}
