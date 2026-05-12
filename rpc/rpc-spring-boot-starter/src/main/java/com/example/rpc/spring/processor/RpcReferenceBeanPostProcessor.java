package com.example.rpc.spring.processor;

import com.example.rpc.client.RpcClientProxy;
import com.example.rpc.registry.RegistryService;
import com.example.rpc.spring.annotation.RpcReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;

import java.lang.reflect.Field;

/**
 * 扫描 @RpcReference 注解并注入远程服务代理
 */
public class RpcReferenceBeanPostProcessor implements BeanPostProcessor, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RpcReferenceBeanPostProcessor.class);

    private final RegistryService registryService;

    public RpcReferenceBeanPostProcessor(RegistryService registryService) {
        this.registryService = registryService;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clazz = bean.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            RpcReference annotation = field.getAnnotation(RpcReference.class);
            if (annotation == null) {
                continue;
            }

            RpcClientProxy proxyFactory = new RpcClientProxy(registryService);
            Object proxy = proxyFactory.create(field.getType());

            field.setAccessible(true);
            try {
                field.set(bean, proxy);
                log.info("injected RPC proxy for field {}.{}", clazz.getSimpleName(), field.getName());
            } catch (IllegalAccessException e) {
                log.error("failed to inject RPC proxy for {}", field.getName(), e);
            }
        }
        return bean;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
