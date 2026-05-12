package com.example.rpc.core.protocol;

/**
 * 消息类型
 */
public enum MessageType {

    REQUEST(0),
    RESPONSE(1),
    HEARTBEAT(2);

    private final int code;

    MessageType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static MessageType fromCode(int code) {
        for (MessageType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("unknown message type: " + code);
    }
}
