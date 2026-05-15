package com.example.circuitbreaker.aspect;

import com.example.circuitbreaker.annotation.CircuitBreaker;
import com.example.circuitbreaker.exception.CircuitBreakerException;
import com.example.circuitbreaker.strategy.CircuitBreakerStateMachine;
import com.example.circuitbreaker.strategy.RejectedStrategy;
import com.example.circuitbreaker.util.KeyParser;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

@Aspect
public class CircuitBreakerAspect {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerAspect.class);

    private final CircuitBreakerStateMachine stateMachine;

    public CircuitBreakerAspect(CircuitBreakerStateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    @Around("@annotation(com.example.circuitbreaker.annotation.CircuitBreaker)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        CircuitBreaker annotation = method.getAnnotation(CircuitBreaker.class);
        // annotation guaranteed non-null by @Around pointcut

        String key = KeyParser.parse(annotation.key(), method, pjp.getArgs());
        boolean acquired = stateMachine.tryAcquire(key, annotation.failureThreshold(),
                annotation.successThreshold(), annotation.timeout(), annotation.timeUnit());

        if (!acquired) {
            return handleRejected(pjp, method, annotation, null);
        }

        try {
            Object result = pjp.proceed();
            stateMachine.recordSuccess(key);
            return result;
        } catch (Throwable t) {
            stateMachine.recordFailure(key);
            return handleRejected(pjp, method, annotation, t);
        }
    }

    private Object handleRejected(ProceedingJoinPoint pjp, Method method,
                                   CircuitBreaker annotation, Throwable originalCause) throws Throwable {
        RejectedStrategy strategy = annotation.rejectedStrategy();

        switch (strategy) {
            case SILENT:
                if (originalCause != null) {
                    log.warn("circuit breaker triggered, request blocked", originalCause);
                }
                return null;
            case CALLER_RUNS:
                // let the caller execute directly (no circuit breaker protection)
                return pjp.proceed();
            case ABORT:
                String fallback = annotation.fallback();
                if (!fallback.isBlank()) {
                    Method fallbackMethod = findFallbackMethod(pjp.getTarget().getClass(), fallback, method.getParameterTypes());
                    if (fallbackMethod != null) {
                        return fallbackMethod.invoke(pjp.getTarget(), pjp.getArgs());
                    }
                }
                CircuitBreakerException ex = new CircuitBreakerException(annotation.message());
                if (originalCause != null) {
                    ex.addSuppressed(originalCause);
                }
                throw ex;
            default:
                throw new CircuitBreakerException(annotation.message());
        }
    }

    @SuppressWarnings("deprecation")
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
