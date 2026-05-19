package com.example.ratelimit.aspect;

import com.example.ratelimit.annotation.RateLimit;
import com.example.ratelimit.exception.RateLimitException;
import com.example.ratelimit.strategy.StrategyFactory;
import com.example.ratelimit.util.KeyParser;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Aspect
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    private final StrategyFactory strategyFactory;

    public RateLimitAspect(StrategyFactory strategyFactory) {
        this.strategyFactory = strategyFactory;
    }

    @Around("@annotation(com.example.ratelimit.annotation.RateLimit) || @within(com.example.ratelimit.annotation.RateLimit)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        Method signatureMethod = ((MethodSignature) pjp.getSignature()).getMethod();
        Class<?> targetClass = pjp.getTarget().getClass();
        Method method = AopUtils.getMostSpecificMethod(signatureMethod, targetClass);
        RateLimit annotation = resolveAnnotation(method, targetClass);

        String key = KeyParser.parse(annotation.key(), method, pjp.getArgs());

        boolean acquired = strategyFactory.getStrategy(annotation).tryAcquire(key, 1);

        if (!acquired) {
            return handleRejected(pjp, method, annotation);
        }

        return pjp.proceed();
    }

    private Object handleRejected(ProceedingJoinPoint pjp, Method method, RateLimit annotation) throws Throwable {
        return switch (annotation.rejectedStrategy()) {
            case SILENT -> null;
            case CALLER_RUNS -> invokeFallback(pjp, method, annotation);
            default -> throw new RateLimitException(annotation.message());
        };
    }

    private Object invokeFallback(ProceedingJoinPoint pjp, Method method, RateLimit annotation) throws Throwable {
        String fallback = annotation.fallback();
        if (fallback.isBlank()) {
            throw new RateLimitException(annotation.message());
        }
        Method fallbackMethod = findFallbackMethod(pjp.getTarget().getClass(), fallback, method.getParameterTypes());
        if (fallbackMethod == null) {
            throw new RateLimitException("fallback method '" + fallback + "' not found");
        }
        try {
            return fallbackMethod.invoke(pjp.getTarget(), pjp.getArgs());
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    private RateLimit resolveAnnotation(Method method, Class<?> targetClass) {
        RateLimit annotation = AnnotatedElementUtils.findMergedAnnotation(method, RateLimit.class);
        if (annotation != null) {
            return annotation;
        }
        return AnnotatedElementUtils.findMergedAnnotation(targetClass, RateLimit.class);
    }

    private Method findFallbackMethod(Class<?> targetClass, String name, Class<?>[] paramTypes) {
        Class<?> current = targetClass;
        while (current != null && current != Object.class) {
            try {
                Method method = current.getDeclaredMethod(name, paramTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException e) {
                current = current.getSuperclass();
            }
        }
        log.warn("fallback method '{}' not found in {} or its parents", name, targetClass);
        return null;
    }
}
