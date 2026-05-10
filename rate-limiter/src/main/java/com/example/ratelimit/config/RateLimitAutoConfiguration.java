package com.example.ratelimit.config;

import com.example.ratelimit.aspect.RateLimitAspect;
import com.example.ratelimit.strategy.StrategyFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class RateLimitAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public StrategyFactory strategyFactory() {
        return new StrategyFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimitAspect rateLimitAspect(StrategyFactory strategyFactory) {
        return new RateLimitAspect(strategyFactory);
    }
}
