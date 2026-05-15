package com.example.idempotent.config;

import com.example.idempotent.aspect.IdempotentAspect;
import com.example.idempotent.strategy.IdempotentStorage;
import com.example.idempotent.strategy.InMemoryIdempotentStorage;
import com.example.idempotent.strategy.RedisIdempotentStorage;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration
public class IdempotentAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
    @ConditionalOnBean(StringRedisTemplate.class)
    static class RedisStorageConfiguration {

        @Bean
        public IdempotentStorage idempotentStorage(StringRedisTemplate redisTemplate) {
            return new RedisIdempotentStorage(redisTemplate);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(IdempotentStorage.class)
    static class InMemoryStorageConfiguration {

        @Bean
        public IdempotentStorage idempotentStorage() {
            return new InMemoryIdempotentStorage();
        }
    }

    @Bean
    public IdempotentAspect idempotentAspect(IdempotentStorage storage) {
        return new IdempotentAspect(storage);
    }
}
