package com.example.rpc;

public class ServiceAddress {

    private final String host;
    private final int port;

    public ServiceAddress(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static ServiceAddress parse(String address) {
        String[] parts = address.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("invalid address: " + address);
        }
        return new ServiceAddress(parts[0], Integer.parseInt(parts[1]));
    }

    public String getHost() { return host; }
    public int getPort() { return port; }

    @Override
    public String toString() {
        return host + ":" + port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServiceAddress that)) return false;
        return port == that.port && host.equals(that.host);
    }

    @Override
    public int hashCode() {
        return 31 * host.hashCode() + port;
    }
}
