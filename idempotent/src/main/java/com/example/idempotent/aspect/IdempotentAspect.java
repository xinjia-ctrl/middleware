package com.example.idempotent.aspect;

import com.example.idempotent.annotation.Idempotent;
import com.example.idempotent.exception.DuplicateRequestException;
import com.example.idempotent.strategy.IdempotentStorage;
import com.example.idempotent.util.KeyParser;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

@Aspect
public class IdempotentAspect {

    private final IdempotentStorage storage;

    public IdempotentAspect(IdempotentStorage storage) {
        this.storage = storage;
    }

    @Around("@annotation(com.example.idempotent.annotation.Idempotent)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Idempotent annotation = method.getAnnotation(Idempotent.class);

        if (annotation == null) {
            return pjp.proceed();
        }

        String key = KeyParser.parse(annotation.key(), method, pjp.getArgs());
        boolean saved = storage.trySave(key, annotation.ttl(), annotation.timeUnit());

        if (!saved) {
            throw new DuplicateRequestException(annotation.message());
        }

        try {
            return pjp.proceed();
        } catch (Throwable t) {
            storage.remove(key);
            throw t;
        }
    }
}
