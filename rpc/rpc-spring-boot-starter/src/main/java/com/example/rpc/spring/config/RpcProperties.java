package com.example.rpc.spring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RPC 框架配置属性
 */
@ConfigurationProperties(prefix = "rpc")
public class RpcProperties {

    /**
     * 注册中心类型：local / redis
     */
    private String registry = "local";

    /**
     * Redis 注册中心配置
     */
    private RedisRegistry redis = new RedisRegistry();

    /**
     * RPC 服务端端口
     */
    private int serverPort = 9090;

    /**
     * 本机服务地址
     */
    private String serverHost = "127.0.0.1";

    public String getRegistry() {
        return registry;
    }

    public void setRegistry(String registry) {
        this.registry = registry;
    }

    public RedisRegistry getRedis() {
        return redis;
    }

    public void setRedis(RedisRegistry redis) {
        this.redis = redis;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public static class RedisRegistry {
        private String host = "127.0.0.1";
        private int port = 6379;

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
    }
}
