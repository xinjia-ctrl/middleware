package com.example.cacheconsistency.config;

import com.example.cacheconsistency.aspect.CacheConsistentAspect;
import com.example.cacheconsistency.strategy.CacheStorage;
import com.example.cacheconsistency.strategy.InMemoryCacheStorage;
import com.example.cacheconsistency.strategy.RedisCacheStorage;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@AutoConfiguration
@EnableConfigurationProperties(CacheConsistentProperties.class)
public class CacheConsistentAutoConfiguration {

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnMissingBean(CacheStorage.class)
    public CacheStorage redisCacheStorage(StringRedisTemplate redisTemplate) {
        return new RedisCacheStorage(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(CacheStorage.class)
    public CacheStorage inMemoryCacheStorage() {
        return new InMemoryCacheStorage();
    }

    @Bean
    public CacheConsistentAspect cacheConsistentAspect(CacheStorage cacheStorage,
                                                       CacheConsistentProperties properties) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cache-consistency-scheduler");
            t.setDaemon(true);
            return t;
        });
        return new CacheConsistentAspect(cacheStorage, scheduler, properties.getKeyPrefix());
    }
}
