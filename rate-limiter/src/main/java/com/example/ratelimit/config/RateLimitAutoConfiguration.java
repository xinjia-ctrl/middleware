package com.example.ratelimit.config;

import com.example.ratelimit.aspect.RateLimitAspect;
import com.example.ratelimit.strategy.StrategyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration
public class RateLimitAutoConfiguration {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Bean
    @ConditionalOnMissingBean
    public StrategyFactory strategyFactory() {
        return new StrategyFactory(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimitAspect rateLimitAspect(StrategyFactory strategyFactory) {
        return new RateLimitAspect(strategyFactory);
    }
}
