package com.example.rpc.spring.annotation;

import com.example.rpc.spring.config.RpcAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用 RPC 框架
 *
 * 在 Spring Boot 启动类上标注此注解即可开启 RPC 功能
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(RpcAutoConfiguration.class)
public @interface EnableRpc {

    /**
     * 服务端端口（仅服务端需要）
     */
    int serverPort() default 9090;
}
