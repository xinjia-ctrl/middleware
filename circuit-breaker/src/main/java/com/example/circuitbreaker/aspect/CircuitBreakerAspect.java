package com.example.circuitbreaker.aspect;

import com.example.circuitbreaker.annotation.CircuitBreaker;
import com.example.circuitbreaker.exception.CircuitBreakerException;
import com.example.circuitbreaker.strategy.CircuitBreakerStateMachine;
import com.example.circuitbreaker.util.KeyParser;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

@Aspect
public class CircuitBreakerAspect {

    private final CircuitBreakerStateMachine stateMachine;

    public CircuitBreakerAspect(CircuitBreakerStateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    @Around("@annotation(com.example.circuitbreaker.annotation.CircuitBreaker)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        CircuitBreaker annotation = method.getAnnotation(CircuitBreaker.class);

        if (annotation == null) {
            return pjp.proceed();
        }

        String key = KeyParser.parse(annotation.key(), method, pjp.getArgs());
        boolean acquired = stateMachine.tryAcquire(key, annotation.failureThreshold(), annotation.successThreshold(), annotation.timeout(), annotation.timeUnit());

        if (!acquired) {
            String fallback = annotation.fallback();
            if (!fallback.isBlank()) {
                Method fallbackMethod = pjp.getTarget().getClass()
                        .getDeclaredMethod(fallback, method.getParameterTypes());
                fallbackMethod.setAccessible(true);
                return fallbackMethod.invoke(pjp.getTarget(), pjp.getArgs());
            }
            throw new CircuitBreakerException(annotation.message());
        }

        try {
            Object result = pjp.proceed();
            stateMachine.recordSuccess(key);
            return result;
        } catch (Throwable t) {
            stateMachine.recordFailure(key);
            String fallback = annotation.fallback();
            if (!fallback.isBlank()) {
                Method fallbackMethod = pjp.getTarget().getClass()
                        .getDeclaredMethod(fallback, method.getParameterTypes());
                fallbackMethod.setAccessible(true);
                return fallbackMethod.invoke(pjp.getTarget(), pjp.getArgs());
            }
            throw t;
        }
    }
}
