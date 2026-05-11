package com.example.circuitbreaker.strategy;

public enum CircuitBreakerState {
    CLOSED,
    OPEN,
    HALF_OPEN
}
