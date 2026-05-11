package com.example.idempotent.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    String key() default "";

    long ttl() default 24;

    TimeUnit timeUnit() default TimeUnit.HOURS;

    String message() default "重复请求，请勿重复提交";

    /**
     * 是否缓存第一次执行的结果，重复请求时直接返回缓存结果而非抛异常
     */
    boolean cacheResult() default false;
}
