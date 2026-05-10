package com.example.ratelimit.annotation;

import com.example.ratelimit.strategy.RejectedStrategy;
import com.example.ratelimit.strategy.StrategyType;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface RateLimit {

    StrategyType strategy() default StrategyType.TOKEN_BUCKET;

    long permits() default 100;

    long window() default 1;

    TimeUnit timeUnit() default TimeUnit.SECONDS;

    double permitsPerSecond() default 10.0;

    int capacity() default 100;

    double leakRate() default 10.0;

    int leakCapacity() default 100;

    String key() default "";

    String fallback() default "";

    RejectedStrategy rejectedStrategy() default RejectedStrategy.ABORT;

    String message() default "Too many requests, please try again later";
}
