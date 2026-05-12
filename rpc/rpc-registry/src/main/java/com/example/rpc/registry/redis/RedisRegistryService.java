package com.example.rpc.registry.redis;

import com.example.rpc.core.serialization.JsonSerializer;
import com.example.rpc.core.serialization.Serializer;
import com.example.rpc.registry.RegistryService;
import com.example.rpc.registry.ServiceMeta;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 基于 Redis 的注册中心实现
 *
 * 服务注册信息以 Hash 结构存储：
 *   key:  "rpc:services:{serviceName}"
 *   field: "{host}:{port}"
 *   value: JSON 序列化的 ServiceMeta
 */
public class RedisRegistryService implements RegistryService {

    private static final String SERVICE_PREFIX = "rpc:services:";
    private static final long DEFAULT_TTL = 30; // seconds

    private final StringRedisTemplate redisTemplate;
    private final Serializer serializer = new JsonSerializer();

    public RedisRegistryService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void register(ServiceMeta serviceMeta) {
        String key = buildKey(serviceMeta.getServiceName());
        String field = buildField(serviceMeta);
        String value = new String(serializer.serialize(serviceMeta));
        redisTemplate.opsForHash().put(key, field, value);
        redisTemplate.expire(key, DEFAULT_TTL, TimeUnit.SECONDS);
    }

    @Override
    public void unregister(ServiceMeta serviceMeta) {
        String key = buildKey(serviceMeta.getServiceName());
        String field = buildField(serviceMeta);
        redisTemplate.opsForHash().delete(key, field);
    }

    @Override
    public List<ServiceMeta> discover(String serviceName) {
        String key = buildKey(serviceName);
        List<Object> values = redisTemplate.opsForHash().values(key);
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return values.stream()
                .map(v -> serializer.deserialize(v.toString().getBytes(), ServiceMeta.class))
                .collect(Collectors.toList());
    }

    @Override
    public void close() {
        // RedisTemplate 由 Spring 管理，不在此关闭
    }

    private String buildKey(String serviceName) {
        return SERVICE_PREFIX + serviceName;
    }

    private String buildField(ServiceMeta meta) {
        return meta.getHost() + ":" + meta.getPort();
    }
}
