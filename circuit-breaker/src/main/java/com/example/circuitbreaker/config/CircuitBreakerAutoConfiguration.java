package com.example.circuitbreaker.config;

import com.example.circuitbreaker.aspect.CircuitBreakerAspect;
import com.example.circuitbreaker.strategy.CircuitBreakerStateMachine;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class CircuitBreakerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerStateMachine circuitBreakerStateMachine() {
        return new CircuitBreakerStateMachine();
    }

    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerAspect circuitBreakerAspect(CircuitBreakerStateMachine stateMachine) {
        return new CircuitBreakerAspect(stateMachine);
    }
}
