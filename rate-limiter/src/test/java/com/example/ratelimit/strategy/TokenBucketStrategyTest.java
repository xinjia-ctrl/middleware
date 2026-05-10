package com.example.ratelimit.strategy;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TokenBucketStrategyTest {

    @Test
    void shouldAcquireWhenTokensAvailable() {
        TokenBucketStrategy strategy = new TokenBucketStrategy(100, 10);
        assertTrue(strategy.tryAcquire("key", 1));
    }

    @Test
    void shouldRejectWhenTokensExhausted() {
        TokenBucketStrategy strategy = new TokenBucketStrategy(100, 3);
        assertTrue(strategy.tryAcquire("key", 3));
        assertFalse(strategy.tryAcquire("key", 1));
    }

    @Test
    void shouldRefillOverTime() throws InterruptedException {
        TokenBucketStrategy strategy = new TokenBucketStrategy(100, 5);
        strategy.tryAcquire("key", 5);
        assertFalse(strategy.tryAcquire("key", 1));

        Thread.sleep(50);

        assertTrue(strategy.tryAcquire("key", 1));
    }

    @Test
    void shouldNotExceedCapacity() throws InterruptedException {
        TokenBucketStrategy strategy = new TokenBucketStrategy(100, 5);
        strategy.tryAcquire("key", 5);
        Thread.sleep(200);
        // rate=100/s, 200ms=20 tokens, capped at capacity=5
        assertTrue(strategy.tryAcquire("key", 5));
        assertFalse(strategy.tryAcquire("key", 1));
    }

    @Test
    void differentKeysShouldNotAffectEachOther() {
        TokenBucketStrategy strategy = new TokenBucketStrategy(100, 3);
        assertTrue(strategy.tryAcquire("user-a", 3));
        assertTrue(strategy.tryAcquire("user-b", 3));
        assertFalse(strategy.tryAcquire("user-a", 1));
        assertFalse(strategy.tryAcquire("user-b", 1));
    }

    @Test
    void shouldHandleConcurrentAccess() throws InterruptedException {
        TokenBucketStrategy strategy = new TokenBucketStrategy(1000, 100);
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                if (strategy.tryAcquire("key", 1)) {
                    successCount.incrementAndGet();
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        assertTrue(successCount.get() <= 100);
    }

    @Test
    void shouldReturnCorrectType() {
        TokenBucketStrategy strategy = new TokenBucketStrategy(10, 5);
        assertEquals(StrategyType.TOKEN_BUCKET, strategy.getType());
    }
}
