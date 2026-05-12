package com.example.rpc.spring.config;

import com.example.rpc.registry.RegistryService;
import com.example.rpc.registry.local.LocalRegistryService;
import com.example.rpc.registry.redis.RedisRegistryService;
import com.example.rpc.spring.processor.RpcReferenceBeanPostProcessor;
import com.example.rpc.spring.processor.RpcServiceBeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * RPC 框架自动配置
 */
@AutoConfiguration
@EnableConfigurationProperties(RpcProperties.class)
public class RpcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RegistryService.class)
    @ConditionalOnProperty(name = "rpc.registry", havingValue = "redis", matchIfMissing = false)
    public RegistryService redisRegistryService(StringRedisTemplate redisTemplate) {
        return new RedisRegistryService(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(RegistryService.class)
    public RegistryService localRegistryService() {
        return new LocalRegistryService();
    }

    @Bean
    public RpcServiceBeanPostProcessor rpcServiceBeanPostProcessor(
            RegistryService registryService, RpcProperties properties) {
        return new RpcServiceBeanPostProcessor(registryService, properties);
    }

    @Bean
    public RpcReferenceBeanPostProcessor rpcReferenceBeanPostProcessor(
            RegistryService registryService) {
        return new RpcReferenceBeanPostProcessor(registryService);
    }
}
