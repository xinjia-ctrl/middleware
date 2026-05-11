package com.example.circuitbreaker.config;

import com.example.circuitbreaker.exception.CircuitBreakerException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CircuitBreakerException.class)
    public ResponseEntity<Map<String, Object>> handleCircuitBreaker(CircuitBreakerException e) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "code", 503,
                        "message", e.getMessage()
                ));
    }
}
