package com.example.rpc.protocol;
import com.example.rpc.config.RpcConstants;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPOutputStream;

public class MyEncode extends MessageToByteEncoder<Object> {

    private final Serializer serializer;

    public MyEncode(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        byte[] body;
        byte compressFlag = 0;

        if (msg instanceof RpcHeartbeat) {
            out.writeShort(RpcConstants.MAGIC_NUMBER);
            out.writeShort(RpcConstants.PROTOCOL_VERSION);
            out.writeShort(MessageType.HEARTBEAT);
            out.writeShort(0);
            out.writeByte(0);
            out.writeByte(0);
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

        body = serializer.serialize(msg);

        if (body.length > RpcConstants.GZIP_THRESHOLD) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(body.length);
            try (GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
                gzip.write(body);
            }
            body = bos.toByteArray();
            compressFlag = RpcConstants.GZIP_COMPRESS;
        }

        out.writeShort(RpcConstants.MAGIC_NUMBER);
        out.writeShort(RpcConstants.PROTOCOL_VERSION);
        out.writeShort(messageType);
        out.writeShort(serializer.getType());
        out.writeByte(compressFlag);
        out.writeByte(0);
        out.writeInt(body.length);
        out.writeBytes(body);
    }
}
