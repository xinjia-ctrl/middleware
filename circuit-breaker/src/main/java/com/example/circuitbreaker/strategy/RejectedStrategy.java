package com.example.circuitbreaker.strategy;

public enum RejectedStrategy {
    ABORT,
    SILENT,
    CALLER_RUNS
}
