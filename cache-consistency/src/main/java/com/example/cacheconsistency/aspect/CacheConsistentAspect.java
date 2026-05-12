package com.example.cacheconsistency.aspect;

import com.example.cacheconsistency.annotation.CacheConsistent;
import com.example.cacheconsistency.strategy.CacheAction;
import com.example.cacheconsistency.strategy.CacheConsistentStrategy;
import com.example.cacheconsistency.util.KeyParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.lang.reflect.Method;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Aspect
public class CacheConsistentAspect {

    private static final String KEY_PREFIX = "cache:";

    private final StringRedisTemplate redisTemplate;
    private final ScheduledExecutorService scheduler;

    public CacheConsistentAspect(StringRedisTemplate redisTemplate, ScheduledExecutorService scheduler) {
        this.redisTemplate = redisTemplate;
        this.scheduler = scheduler;
    }

    @Around("@annotation(com.example.cacheconsistency.annotation.CacheConsistent)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        CacheConsistent annotation = method.getAnnotation(CacheConsistent.class);

        if (annotation == null) {
            return pjp.proceed();
        }

        String cacheKey = KEY_PREFIX + KeyParser.parse(annotation.key(), method, pjp.getArgs());

        if (annotation.strategy() == CacheConsistentStrategy.DOUBLE_DELETE) {
            return handleDoubleDelete(pjp, cacheKey, annotation);
        }

        if (annotation.action() == CacheAction.PUT) {
            return handlePut(pjp, cacheKey, annotation);
        }

        return handleEvictAfter(pjp, cacheKey);
    }

    private Object handleEvictAfter(ProceedingJoinPoint pjp, String cacheKey) throws Throwable {
        try {
            return pjp.proceed();
        } finally {
            redisTemplate.delete(cacheKey);
        }
    }

    private Object handlePut(ProceedingJoinPoint pjp, String cacheKey, CacheConsistent annotation) throws Throwable {
        Object result = pjp.proceed();
        String value = serializeResult(result, annotation);
        redisTemplate.opsForValue().set(cacheKey, value, annotation.ttl(), annotation.timeUnit());
        return result;
    }

    private Object handleDoubleDelete(ProceedingJoinPoint pjp, String cacheKey, CacheConsistent annotation) throws Throwable {
        redisTemplate.delete(cacheKey);

        try {
            return pjp.proceed();
        } finally {
            redisTemplate.delete(cacheKey);
            scheduler.schedule(() -> redisTemplate.delete(cacheKey),
                    annotation.delayMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }

    private String serializeResult(Object result, CacheConsistent annotation) {
        if (result instanceof String) {
            return (String) result;
        }
        try {
            return new ObjectMapper().writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化缓存结果失败", e);
        }
    }
}
