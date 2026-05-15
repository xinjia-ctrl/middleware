package com.example.cacheconsistency.aspect;

import com.example.cacheconsistency.annotation.CacheConsistent;
import com.example.cacheconsistency.strategy.CacheAction;
import com.example.cacheconsistency.strategy.CacheConsistentStrategy;
import com.example.cacheconsistency.strategy.CacheStorage;
import com.example.cacheconsistency.util.KeyParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Aspect
public class CacheConsistentAspect {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final CacheStorage cacheStorage;
    private final ScheduledExecutorService scheduler;
    private final String keyPrefix;

    public CacheConsistentAspect(CacheStorage cacheStorage, ScheduledExecutorService scheduler, String keyPrefix) {
        this.cacheStorage = cacheStorage;
        this.scheduler = scheduler;
        this.keyPrefix = keyPrefix;
    }

    @Around("@annotation(com.example.cacheconsistency.annotation.CacheConsistent)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        CacheConsistent annotation = method.getAnnotation(CacheConsistent.class);

        String cacheKey = keyPrefix + KeyParser.parse(annotation.key(), method, pjp.getArgs());

        return switch (annotation.strategy()) {
            case DOUBLE_DELETE -> handleDoubleDelete(pjp, cacheKey, annotation);
            case EVICT_AFTER -> handleEvictAfter(pjp, cacheKey, annotation);
        };
    }

    private Object handleEvictAfter(ProceedingJoinPoint pjp, String cacheKey, CacheConsistent annotation) throws Throwable {
        if (annotation.action() == CacheAction.PUT) {
            return handlePut(pjp, cacheKey, annotation);
        }
        try {
            return pjp.proceed();
        } finally {
            cacheStorage.delete(cacheKey);
        }
    }

    private Object handlePut(ProceedingJoinPoint pjp, String cacheKey, CacheConsistent annotation) throws Throwable {
        Object result = pjp.proceed();
        String value = serializeResult(result);
        cacheStorage.set(cacheKey, value, annotation.ttl(), annotation.timeUnit());
        return result;
    }

    private Object handleDoubleDelete(ProceedingJoinPoint pjp, String cacheKey, CacheConsistent annotation) throws Throwable {
        cacheStorage.delete(cacheKey);

        try {
            Object result = pjp.proceed();
            if (annotation.action() == CacheAction.PUT) {
                String value = serializeResult(result);
                cacheStorage.set(cacheKey, value, annotation.ttl(), annotation.timeUnit());
            }
            return result;
        } finally {
            cacheStorage.delete(cacheKey);
            scheduler.schedule(() -> cacheStorage.delete(cacheKey),
                    annotation.delayMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private String serializeResult(Object result) {
        if (result instanceof String) {
            return (String) result;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化缓存结果失败", e);
        }
    }
}
