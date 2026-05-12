package com.example.rpc.transport.netty;

import com.example.rpc.core.protocol.MessageType;
import com.example.rpc.core.serialization.Serializer;
import com.example.rpc.transport.netty.codec.RpcMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * RPC 消息解码器（入站）
 *
 * 协议格式（共 17 字节头）：
 * magic(2) + version(1) + msgType(1) + serialType(1) + requestId(8) + bodyLen(4)
 */
public class RpcMessageDecoder extends ReplayingDecoder<Void> {

    private static final Logger log = LoggerFactory.getLogger(RpcMessageDecoder.class);
    private static final int HEADER_LENGTH = 17;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        short magic = in.readShort();
        if (magic != RpcMessage.MAGIC) {
            throw new RuntimeException("invalid magic: " + magic);
        }

        byte version = in.readByte();
        byte msgTypeCode = in.readByte();
        byte serialType = in.readByte();
        long requestId = in.readLong();
        int bodyLength = in.readInt();

        byte[] body = new byte[bodyLength];
        in.readBytes(body);

        RpcMessage message = new RpcMessage(MessageType.fromCode(msgTypeCode), serialType, requestId, body);
        message.setVersion(version);
        out.add(message);
    }
}
