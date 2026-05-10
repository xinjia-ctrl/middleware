package com.example.ratelimit.aspect;

import com.example.ratelimit.annotation.RateLimit;
import com.example.ratelimit.exception.RateLimitException;
import com.example.ratelimit.strategy.RejectedStrategy;
import com.example.ratelimit.strategy.StrategyFactory;
import com.example.ratelimit.util.KeyParser;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

@Aspect
public class RateLimitAspect {

    private final StrategyFactory strategyFactory;

    public RateLimitAspect(StrategyFactory strategyFactory) {
        this.strategyFactory = strategyFactory;
    }

    @Around("@annotation(com.example.ratelimit.annotation.RateLimit)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        RateLimit annotation = method.getAnnotation(RateLimit.class);

        if (annotation == null) {
            return pjp.proceed();
        }

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
        Method fallbackMethod = pjp.getTarget().getClass()
                .getDeclaredMethod(fallback, method.getParameterTypes());
        fallbackMethod.setAccessible(true);
        return fallbackMethod.invoke(pjp.getTarget(), pjp.getArgs());
    }
}
