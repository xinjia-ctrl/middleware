package com.example.cacheconsistency.annotation;

import com.example.cacheconsistency.strategy.CacheAction;
import com.example.cacheconsistency.strategy.CacheConsistentStrategy;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheConsistent {

    /**
     * 缓存 key，支持 SpEL 表达式
     */
    String key();

    /**
     * 缓存操作：EVICT（淘汰）、PUT（写入）
     */
    CacheAction action() default CacheAction.EVICT;

    /**
     * 一致性策略：EVICT_AFTER（先写后删）、DOUBLE_DELETE（延迟双删）
     */
    CacheConsistentStrategy strategy() default CacheConsistentStrategy.EVICT_AFTER;

    /**
     * 写入缓存的表达式（PUT 时使用），默认取方法返回值
     */
    String result() default "";

    /**
     * 缓存 TTL（PUT 时有效）
     */
    long ttl() default 1;

    /**
     * 缓存 TTL 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.HOURS;

    /**
     * 延迟双删的等待时间（毫秒）
     */
    long delayMillis() default 500;
}
