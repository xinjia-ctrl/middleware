package com.example.rpc.registry;

import com.example.rpc.config.RpcConstants;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ZkServiceRegister implements ServiceRegister {

    private static final Logger log = LoggerFactory.getLogger(ZkServiceRegister.class);
    private final CuratorFramework client;

    public ZkServiceRegister(String zkAddr) {
        this.client = CuratorClientFactory.getClient(zkAddr);
        createRootIfNeeded();
    }

    private void createRootIfNeeded() {
        try {
            if (client.checkExists().forPath(RpcConstants.ZK_ROOT) == null) {
                client.create().creatingParentsIfNeeded()
                        .forPath(RpcConstants.ZK_ROOT);
            }
        } catch (Exception e) {
            throw new RuntimeException("failed to create ZK root path", e);
        }
    }

    @Override
    public void register(String serviceName, String address) {
        try {
            String path = RpcConstants.ZK_ROOT + "/" + serviceName + "/" + RpcConstants.ZK_PROVIDERS + "/" + address;
            if (client.checkExists().forPath(path) == null) {
                client.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.EPHEMERAL)
                        .forPath(path);
            }
            log.info("registered to ZK: {}", path);
        } catch (Exception e) {
            throw new RuntimeException("failed to register service to ZK", e);
        }
    }

    @Override
    public List<String> discover(String serviceName) {
        try {
            String path = RpcConstants.ZK_ROOT + "/" + serviceName + "/" + RpcConstants.ZK_PROVIDERS;
            return client.getChildren().forPath(path);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public void close() {
        // CuratorClientFactory manages lifecycle
    }
}
