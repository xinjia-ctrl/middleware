package com.example.rpc;

public class RpcClientConfig {

    private String zkAddr = "127.0.0.1:2181";
    private String loadBalance = "random";
    private Serializer serializer = new ObjectSerializer();
    private int timeout = 3;

    public RpcClientConfig() {
    }

    public RpcClientConfig(String zkAddr) {
        this.zkAddr = zkAddr;
    }

    public String getZkAddr() { return zkAddr; }
    public void setZkAddr(String zkAddr) { this.zkAddr = zkAddr; }

    public String getLoadBalance() { return loadBalance; }
    public void setLoadBalance(String loadBalance) { this.loadBalance = loadBalance; }

    public Serializer getSerializer() { return serializer; }
    public void setSerializer(Serializer serializer) { this.serializer = serializer; }

    public int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }
}
