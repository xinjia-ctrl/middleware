package com.example.rpc.spring.processor;

import com.example.rpc.registry.RegistryService;
import com.example.rpc.server.RpcServiceProvider;
import com.example.rpc.spring.config.RpcProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.annotation.Annotation;

/**
 * 扫描 @RpcService 注解并注册服务
 *
 * 同时支持 com.example.rpc.server.annotation.RpcService（纯 RPC）和
 * com.example.rpc.spring.annotation.RpcService（Spring 整合）两个注解
 */
public class RpcServiceBeanPostProcessor implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(RpcServiceBeanPostProcessor.class);

    private static final Class<? extends Annotation> SPRING_ANNOTATION = com.example.rpc.spring.annotation.RpcService.class;
    private static final Class<? extends Annotation> SERVER_ANNOTATION = com.example.rpc.server.annotation.RpcService.class;

    private final RegistryService registryService;
    private final RpcProperties properties;
    private RpcServiceProvider provider;

    public RpcServiceBeanPostProcessor(RegistryService registryService, RpcProperties properties) {
        this.registryService = registryService;
        this.properties = properties;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Annotation annotation = findRpcServiceAnnotation(bean.getClass());
        if (annotation == null) {
            return bean;
        }

        Class<?> serviceInterface = resolveServiceInterface(bean.getClass(), annotation);
        if (serviceInterface == null) {
            return bean;
        }

        if (provider == null) {
            provider = new RpcServiceProvider(registryService, properties.getServerHost(), properties.getServerPort());
        }

        provider.registerService(serviceInterface, bean);
        provider.start();

        return bean;
    }

    private Annotation findRpcServiceAnnotation(Class<?> clazz) {
        Annotation ann = clazz.getAnnotation(SPRING_ANNOTATION);
        if (ann != null) return ann;
        return clazz.getAnnotation(SERVER_ANNOTATION);
    }

    private Class<?> resolveServiceInterface(Class<?> beanClass, Annotation annotation) {
        if (annotation instanceof com.example.rpc.spring.annotation.RpcService springAnn) {
            Class<?> iface = springAnn.value();
            if (iface != void.class) return iface;
        } else if (annotation instanceof com.example.rpc.server.annotation.RpcService serverAnn) {
            Class<?> iface = serverAnn.value();
            if (iface != void.class) return iface;
        }

        Class<?>[] interfaces = beanClass.getInterfaces();
        if (interfaces.length > 0) {
            return interfaces[0];
        }
        log.warn("no service interface found for {}", beanClass);
        return null;
    }
}
