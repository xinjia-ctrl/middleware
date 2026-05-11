package com.example.idempotent.config;

import com.example.idempotent.aspect.IdempotentAspect;
import com.example.idempotent.strategy.IdempotentStorage;
import com.example.idempotent.strategy.InMemoryIdempotentStorage;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class IdempotentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IdempotentStorage idempotentStorage() {
        return new InMemoryIdempotentStorage();
    }

    @Bean
    @ConditionalOnMissingBean
    public IdempotentAspect idempotentAspect(IdempotentStorage storage) {
        return new IdempotentAspect(storage);
    }
}
