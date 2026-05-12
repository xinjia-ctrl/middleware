package com.example.cacheconsistency.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cache-consistency")
public class CacheConsistentProperties {

    /**
     * 缓存 key 前缀
     */
    private String keyPrefix = "cache:";

    /**
     * 延迟双删默认等待时间（毫秒）
     */
    private long defaultDelayMillis = 500;

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public long getDefaultDelayMillis() {
        return defaultDelayMillis;
    }

    public void setDefaultDelayMillis(long defaultDelayMillis) {
        this.defaultDelayMillis = defaultDelayMillis;
    }
}
