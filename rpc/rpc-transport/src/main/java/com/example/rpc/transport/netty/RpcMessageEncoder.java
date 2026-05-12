package com.example.rpc.transport.netty;

import com.example.rpc.core.protocol.MessageType;
import com.example.rpc.core.protocol.RpcRequest;
import com.example.rpc.core.serialization.Serializer;
import com.example.rpc.transport.netty.codec.RpcMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * RPC 消息编码器（出站）
 */
public class RpcMessageEncoder extends MessageToByteEncoder<RpcMessage> {

    private static final Logger log = LoggerFactory.getLogger(RpcMessageEncoder.class);

    private final Serializer serializer;

    public RpcMessageEncoder(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage msg, ByteBuf out) {
        out.writeShort(RpcMessage.MAGIC);
        out.writeByte(msg.getVersion());
        out.writeByte(msg.getMessageType().getCode());
        out.writeByte(msg.getSerializationType() > 0 ? msg.getSerializationType() : serializer.getType());
        out.writeLong(msg.getRequestId());
        out.writeInt(msg.getBody().length);
        out.writeBytes(msg.getBody());
    }

    public static RpcMessage toRpcMessage(RpcRequest request, Serializer serializer) {
        byte[] body = serializer.serialize(request);
        return new RpcMessage(MessageType.REQUEST, serializer.getType(), requestId(), body);
    }

    private static final AtomicLong REQUEST_ID = new AtomicLong(0);

    public static long requestId() {
        return REQUEST_ID.incrementAndGet();
    }
}
