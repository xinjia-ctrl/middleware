package com.example.cacheconsistency.aspect;

import com.example.cacheconsistency.annotation.CacheConsistent;
import com.example.cacheconsistency.strategy.CacheAction;
import com.example.cacheconsistency.strategy.CacheConsistentStrategy;
import com.example.cacheconsistency.strategy.CacheStorage;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheConsistentAspectTest {

    private static final String TEST_KEY_PREFIX = "cache:";

    @Mock
    private CacheStorage cacheStorage;
    @Mock
    private ScheduledExecutorService scheduler;

    private CacheConsistentAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new CacheConsistentAspect(cacheStorage, scheduler, TEST_KEY_PREFIX);
    }

    // ---- EVICT_AFTER ----

    @Test
    void shouldEvictCacheAfterMethodExecution() throws Throwable {
        ProceedingJoinPoint pjp = mockPjp(TestService.class.getMethod("evictAfterMethod"), "evicted");

        Object result = aspect.around(pjp);

        assertEquals("evicted", result);
        verify(pjp).proceed();
        verify(cacheStorage).delete(contains(TEST_KEY_PREFIX));
    }

    @Test
    void shouldEvictCacheEvenWhenMethodThrows() throws Throwable {
        ProceedingJoinPoint pjp = mockPjp(TestService.class.getMethod("evictAfterMethod"), null);
        when(pjp.proceed()).thenThrow(new RuntimeException("db error"));

        assertThrows(RuntimeException.class, () -> aspect.around(pjp));
        verify(cacheStorage).delete(contains(TEST_KEY_PREFIX));
    }

    // ---- PUT ----

    @Test
    void shouldSetCacheAfterPutAction() throws Throwable {
        ProceedingJoinPoint pjp = mockPjp(TestService.class.getMethod("putMethod"), "put-value");

        Object result = aspect.around(pjp);

        assertEquals("put-value", result);
        verify(cacheStorage).set(contains(TEST_KEY_PREFIX), eq("put-value"), eq(10L), eq(TimeUnit.MINUTES));
    }

    @Test
    void shouldSerializeNonStringResultForPut() throws Throwable {
        ProceedingJoinPoint pjp = mockPjp(TestService.class.getMethod("putObjectMethod"), new Data("hello"));

        Object result = aspect.around(pjp);

        assertInstanceOf(Data.class, result);
        verify(cacheStorage).set(contains(TEST_KEY_PREFIX), contains("hello"), eq(1L), eq(TimeUnit.HOURS));
    }

    // ---- DOUBLE_DELETE ----

    @Test
    void shouldDeleteBeforeAndAfterExecution() throws Throwable {
        ProceedingJoinPoint pjp = mockPjp(TestService.class.getMethod("doubleDeleteMethod"), "double");

        Object result = aspect.around(pjp);

        assertEquals("double", result);
        verify(cacheStorage, times(2)).delete(contains(TEST_KEY_PREFIX));
        verify(scheduler).schedule(any(Runnable.class), eq(300L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void shouldStillDeleteWhenMethodThrows() throws Throwable {
        ProceedingJoinPoint pjp = mockPjp(TestService.class.getMethod("doubleDeleteMethod"), null);
        when(pjp.proceed()).thenThrow(new RuntimeException("err"));

        assertThrows(RuntimeException.class, () -> aspect.around(pjp));
        // before + after (scheduled task is not executed by mock scheduler)
        verify(cacheStorage, times(2)).delete(contains(TEST_KEY_PREFIX));
    }

    // ---- NO ANNOTATION ----

    @Test
    void shouldProceedDirectlyWhenNoAnnotation() throws Throwable {
        Method method = TestService.class.getMethod("noAnnotationMethod");
        MethodSignature sig = mock(MethodSignature.class);
        when(sig.getMethod()).thenReturn(method);

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.getSignature()).thenReturn(sig);
        when(pjp.proceed()).thenReturn("plain");

        Object result = aspect.around(pjp);

        assertEquals("plain", result);
        verifyNoInteractions(cacheStorage);
    }

    // ---- helper ----

    private ProceedingJoinPoint mockPjp(Method method, Object proceedResult) throws Throwable {
        MethodSignature sig = mock(MethodSignature.class);
        when(sig.getMethod()).thenReturn(method);

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.getSignature()).thenReturn(sig);
        if (proceedResult != null) {
            when(pjp.proceed()).thenReturn(proceedResult);
        }
        when(pjp.getArgs()).thenReturn(new Object[]{"unused"});
        return pjp;
    }

    // ---- test service with annotated methods ----

    static class TestService {
        @CacheConsistent(key = "'test:' + #id")
        public String evictAfterMethod() { return "evicted"; }

        @CacheConsistent(key = "'test:' + #id", action = CacheAction.PUT, ttl = 10, timeUnit = TimeUnit.MINUTES)
        public String putMethod() { return "put-value"; }

        @CacheConsistent(key = "'test:' + #id", action = CacheAction.PUT)
        public Data putObjectMethod() { return new Data("hello"); }

        @CacheConsistent(key = "'test:' + #id", strategy = CacheConsistentStrategy.DOUBLE_DELETE, delayMillis = 300)
        public String doubleDeleteMethod() { return "double"; }

        public String noAnnotationMethod() { return "plain"; }
    }

    record Data(String name) {}
}
