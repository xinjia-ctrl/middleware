package com.example.rpc.config;

public class RpcConstants {

    private RpcConstants() {
    }

    public static final String ZK_ROOT = "/middleware-rpc";
    public static final String ZK_PROVIDERS = "providers";
    public static final int ZK_SESSION_TIMEOUT = 10000;
    public static final int ZK_CONNECTION_TIMEOUT = 5000;

    public static final short SERIALIZER_JDK = 0;
    public static final short SERIALIZER_JSON = 1;

    // 协议：魔数 + 版本 + 消息类型 + 序列化方式 + 压缩标记 + 保留 + 消息体长度 = 14B
    public static final short MAGIC_NUMBER = (short) 0xCCDD;
    public static final short PROTOCOL_VERSION = 1;
    public static final int HEADER_SIZE = 14;
    public static final byte GZIP_COMPRESS = 1;
    public static final int GZIP_THRESHOLD = 256;

    public static final String LB_RANDOM = "random";
    public static final String LB_ROUND = "round";
    public static final String LB_CONSISTENT_HASH = "consistentHash";
}
