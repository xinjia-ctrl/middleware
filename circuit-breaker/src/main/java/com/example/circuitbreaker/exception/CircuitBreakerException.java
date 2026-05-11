package com.example.circuitbreaker.exception;

public class CircuitBreakerException extends RuntimeException {

    public CircuitBreakerException(String message) {
        super(message);
    }

    public CircuitBreakerException(String message, Throwable cause) {
        super(message, cause);
    }
}
