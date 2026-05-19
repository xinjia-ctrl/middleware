package com.example.rpc.config;

import com.example.rpc.protocol.ObjectSerializer;
import com.example.rpc.protocol.Serializer;

public class RpcClientConfig {

    private String zkAddr = "127.0.0.1:2181";
    private String loadBalance = "random";
    private Serializer serializer = new ObjectSerializer();
    private int timeoutSeconds = 3;
    private int retryCount = 2;

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

    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final RpcClientConfig config = new RpcClientConfig();

        public Builder zkAddr(String zkAddr) {
            config.setZkAddr(zkAddr);
            return this;
        }

        public Builder loadBalance(String loadBalance) {
            config.setLoadBalance(loadBalance);
            return this;
        }

        public Builder serializer(Serializer serializer) {
            config.setSerializer(serializer);
            return this;
        }

        public Builder timeoutSeconds(int timeoutSeconds) {
            config.setTimeoutSeconds(timeoutSeconds);
            return this;
        }

        public Builder retryCount(int retryCount) {
            config.setRetryCount(retryCount);
            return this;
        }

        public RpcClientConfig build() {
            return config;
        }
    }
}
