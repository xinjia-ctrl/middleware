package com.example.rpc.server.annotation;

import java.lang.annotation.*;

/**
 * 标记 RPC 服务实现类
 *
 * 被标注的 Bean 会自动注册到 RPC 服务端并暴露到注册中心
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RpcService {

    /**
     * 服务接口类，默认取实现类首个接口
     */
    Class<?> value() default void.class;

    /**
     * 服务版本
     */
    String version() default "";
}
