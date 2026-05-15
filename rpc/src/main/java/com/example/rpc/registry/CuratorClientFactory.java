package com.example.rpc.registry;

import com.example.rpc.config.RpcConstants;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CuratorClientFactory {

    private static final Map<String, CuratorFramework> clients = new ConcurrentHashMap<>();

    public static CuratorFramework getClient(String zkAddr) {
        return clients.computeIfAbsent(zkAddr, addr -> {
            RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
            CuratorFramework client = CuratorFrameworkFactory.builder()
                    .connectString(addr)
                    .sessionTimeoutMs(RpcConstants.ZK_SESSION_TIMEOUT)
                    .connectionTimeoutMs(RpcConstants.ZK_CONNECTION_TIMEOUT)
                    .retryPolicy(retryPolicy)
                    .build();
            client.start();
            return client;
        });
    }

    public static void close(String zkAddr) {
        CuratorFramework client = clients.remove(zkAddr);
        if (client != null) {
            client.close();
        }
    }

    public static void closeAll() {
        clients.values().forEach(CuratorFramework::close);
        clients.clear();
    }
}
