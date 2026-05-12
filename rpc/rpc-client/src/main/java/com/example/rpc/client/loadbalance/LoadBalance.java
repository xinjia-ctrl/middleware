package com.example.rpc.client.loadbalance;

import com.example.rpc.registry.ServiceMeta;

import java.util.List;

/**
 * 负载均衡接口
 */
public interface LoadBalance {

    ServiceMeta select(List<ServiceMeta> services);
}
