package com.example.rpc;

public class RpcConstants {

    private RpcConstants() {
    }

    public static final String ZK_ROOT = "/middleware-rpc";
    public static final String ZK_PROVIDERS = "providers";
    public static final int ZK_SESSION_TIMEOUT = 10000;
    public static final int ZK_CONNECTION_TIMEOUT = 5000;

    public static final short SERIALIZER_JDK = 0;
    public static final short SERIALIZER_JSON = 1;

    public static final String LB_RANDOM = "random";
    public static final String LB_ROUND = "round";
    public static final String LB_CONSISTENT_HASH = "consistentHash";
}
