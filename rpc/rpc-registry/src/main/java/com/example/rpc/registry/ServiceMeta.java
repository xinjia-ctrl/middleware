package com.example.rpc.registry;

import java.util.Objects;

/**
 * 服务元数据
 */
public class ServiceMeta {

    private String serviceName;
    private String host;
    private int port;
    private String version;
    private long timestamp;

    public ServiceMeta() {
    }

    public ServiceMeta(String serviceName, String host, int port) {
        this.serviceName = serviceName;
        this.host = host;
        this.port = port;
        this.timestamp = System.currentTimeMillis();
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceMeta that = (ServiceMeta) o;
        return port == that.port && Objects.equals(serviceName, that.serviceName)
                && Objects.equals(host, that.host) && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceName, host, port, version);
    }

    @Override
    public String toString() {
        return serviceName + "@" + host + ":" + port;
    }
}
