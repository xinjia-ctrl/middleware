package com.example.rpc.spring.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Spring 环境下的 RPC 服务注解
 *
 * 组合 @Component，被标注的类会自动注册为 RPC 服务
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface RpcService {

    Class<?> value() default void.class;

    String version() default "";
}
