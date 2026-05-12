package com.example.rpc.transport.netty.codec;

import com.example.rpc.core.protocol.MessageType;

/**
 * RPC 消息结构，对应传输层协议格式
 *
 * <pre>
 * ┌─────────┬──────────┬─────────┬──────────┬──────────┬───────────┐
 * │ magic   │ version  │ msgType │ serial   │ requestId│ bodyLen   │
 * │ (2 byte)│ (1 byte) │ (1 byte)│ (1 byte) │ (8 byte) │ (4 byte)  │
 * ├─────────┴──────────┴─────────┴──────────┴──────────┴───────────┤
 * │                        body (variable)                         │
 * └────────────────────────────────────────────────────────────────┘
 * </pre>
 */
public class RpcMessage {

    public static final short MAGIC = (short) 0xE5E5;

    private byte version = 1;
    private MessageType messageType;
    private byte serializationType;
    private long requestId;
    private byte[] body;

    public RpcMessage() {
    }

    public RpcMessage(MessageType messageType, byte serializationType, long requestId, byte[] body) {
        this.messageType = messageType;
        this.serializationType = serializationType;
        this.requestId = requestId;
        this.body = body;
    }

    public static short getMagic() {
        return MAGIC;
    }

    public byte getVersion() {
        return version;
    }

    public void setVersion(byte version) {
        this.version = version;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public byte getSerializationType() {
        return serializationType;
    }

    public void setSerializationType(byte serializationType) {
        this.serializationType = serializationType;
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }
}
