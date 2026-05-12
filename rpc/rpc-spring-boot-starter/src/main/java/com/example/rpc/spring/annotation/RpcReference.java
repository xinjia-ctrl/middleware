package com.example.rpc.spring.annotation;

import java.lang.annotation.*;

/**
 * 注入 RPC 远程服务代理
 *
 * 标注在字段上，自动生成并注入远程服务的代理对象
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RpcReference {

    String version() default "";
}
