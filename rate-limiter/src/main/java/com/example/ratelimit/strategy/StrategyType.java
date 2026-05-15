package com.example.ratelimit.strategy;

public enum StrategyType {
    TOKEN_BUCKET,
    FIXED_WINDOW,
    SLIDING_WINDOW,
    LEAKY_BUCKET,
    LOCK_FREE
}
