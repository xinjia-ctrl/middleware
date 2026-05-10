package com.example.ratelimit.strategy;

public interface RateLimitStrategy {

    boolean tryAcquire(String key, int permits);

    StrategyType getType();
}
