package com.example.ratelimit.strategy;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SlidingWindowStrategyTest {

    @Test
    void shouldAcquireWithinWindowLimit() {
        SlidingWindowStrategy strategy = new SlidingWindowStrategy(5, 1, TimeUnit.SECONDS);
        assertTrue(strategy.tryAcquire("key", 1));
        assertTrue(strategy.tryAcquire("key", 2));
    }

    @Test
    void shouldRejectWhenWindowLimitExceeded() {
        SlidingWindowStrategy strategy = new SlidingWindowStrategy(3, 1, TimeUnit.SECONDS);
        assertTrue(strategy.tryAcquire("key", 3));
        assertFalse(strategy.tryAcquire("key", 1));
    }

    @Test
    void shouldAllowAfterWindowSlides() throws InterruptedException {
        SlidingWindowStrategy strategy = new SlidingWindowStrategy(2, 100, TimeUnit.MILLISECONDS);
        assertTrue(strategy.tryAcquire("key", 2));
        assertFalse(strategy.tryAcquire("key", 1));

        Thread.sleep(150);

        assertTrue(strategy.tryAcquire("key", 1));
    }

    @Test
    void differentKeysShouldNotAffectEachOther() {
        SlidingWindowStrategy strategy = new SlidingWindowStrategy(2, 1, TimeUnit.SECONDS);
        assertTrue(strategy.tryAcquire("user-a", 2));
        assertTrue(strategy.tryAcquire("user-b", 2));
        assertFalse(strategy.tryAcquire("user-a", 1));
        assertFalse(strategy.tryAcquire("user-b", 1));
    }

    @Test
    void shouldReturnCorrectType() {
        SlidingWindowStrategy strategy = new SlidingWindowStrategy(5, 1, TimeUnit.SECONDS);
        assertEquals(StrategyType.SLIDING_WINDOW, strategy.getType());
    }
}
