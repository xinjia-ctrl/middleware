package com.example.cacheconsistency.config;

import com.example.cacheconsistency.aspect.CacheConsistentAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@AutoConfiguration
public class CacheConsistentAutoConfiguration {

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    public CacheConsistentAspect cacheConsistentAspect(StringRedisTemplate redisTemplate) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cache-consistency-scheduler");
            t.setDaemon(true);
            return t;
        });
        return new CacheConsistentAspect(redisTemplate, scheduler);
    }
}
