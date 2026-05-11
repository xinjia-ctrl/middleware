package com.example.circuitbreaker.annotation;

import com.example.circuitbreaker.strategy.RejectedStrategy;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface CircuitBreaker {

    String fallback() default "";

    RejectedStrategy rejectedStrategy() default RejectedStrategy.ABORT;

    int failureThreshold() default 5;

    int successThreshold() default 3;

    long timeout() default 10;

    TimeUnit timeUnit() default TimeUnit.SECONDS;

    String key() default "";

    String message() default "Circuit breaker is open, request blocked";
}
