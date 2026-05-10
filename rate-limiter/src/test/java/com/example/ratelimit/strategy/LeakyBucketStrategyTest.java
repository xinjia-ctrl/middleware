package com.example.ratelimit.strategy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LeakyBucketStrategyTest {

    @Test
    void shouldAcquireWithinCapacity() {
        LeakyBucketStrategy strategy = new LeakyBucketStrategy(10, 5);
        assertTrue(strategy.tryAcquire("key", 1));
        assertTrue(strategy.tryAcquire("key", 2));
    }

    @Test
    void shouldRejectWhenCapacityExceeded() {
        LeakyBucketStrategy strategy = new LeakyBucketStrategy(10, 3);
        assertTrue(strategy.tryAcquire("key", 3));
        assertFalse(strategy.tryAcquire("key", 1));
    }

    @Test
    void shouldLeakOverTime() throws InterruptedException {
        LeakyBucketStrategy strategy = new LeakyBucketStrategy(100, 5);
        strategy.tryAcquire("key", 5);
        assertFalse(strategy.tryAcquire("key", 1));

        Thread.sleep(50);

        assertTrue(strategy.tryAcquire("key", 1));
    }

    @Test
    void differentKeysShouldNotAffectEachOther() {
        LeakyBucketStrategy strategy = new LeakyBucketStrategy(10, 3);
        assertTrue(strategy.tryAcquire("user-a", 3));
        assertTrue(strategy.tryAcquire("user-b", 3));
        assertFalse(strategy.tryAcquire("user-a", 1));
        assertFalse(strategy.tryAcquire("user-b", 1));
    }

    @Test
    void shouldReturnCorrectType() {
        LeakyBucketStrategy strategy = new LeakyBucketStrategy(10, 5);
        assertEquals(StrategyType.LEAKY_BUCKET, strategy.getType());
    }
}
