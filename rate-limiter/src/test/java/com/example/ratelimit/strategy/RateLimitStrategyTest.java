package com.example.ratelimit.strategy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitStrategyTest {

    static Stream<Arguments> strategies() {
        return Stream.of(
                Arguments.of(new TokenBucketStrategy(100, 3), "TokenBucket"),
                Arguments.of(new FixedWindowStrategy(3, 100, TimeUnit.MILLISECONDS), "FixedWindow"),
                Arguments.of(new SlidingWindowStrategy(3, 100, TimeUnit.MILLISECONDS), "SlidingWindow"),
                Arguments.of(new LeakyBucketStrategy(100, 3), "LeakyBucket")
        );
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("strategies")
    void shouldAcquireWithinLimit(RateLimitStrategy s, String name) {
        assertTrue(s.tryAcquire("key", 1));
        assertTrue(s.tryAcquire("key", 2));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("strategies")
    void shouldRejectWhenExceeded(RateLimitStrategy s, String name) {
        assertTrue(s.tryAcquire("key", 3));
        assertFalse(s.tryAcquire("key", 1));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("strategies")
    void shouldRecoverAfterTimeElapsed(RateLimitStrategy s, String name) throws InterruptedException {
        assertTrue(s.tryAcquire("key", 3));
        assertFalse(s.tryAcquire("key", 1));
        Thread.sleep(200);
        assertTrue(s.tryAcquire("key", 1));
    }

    @Test
    void differentKeysShouldNotAffectEachOther() {
        TokenBucketStrategy s = new TokenBucketStrategy(100, 3);
        assertTrue(s.tryAcquire("user-a", 3));
        assertTrue(s.tryAcquire("user-b", 3));
        assertFalse(s.tryAcquire("user-a", 1));
        assertFalse(s.tryAcquire("user-b", 1));
    }
}
