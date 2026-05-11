package com.example.circuitbreaker.controller;

import com.example.circuitbreaker.annotation.CircuitBreaker;
import com.example.circuitbreaker.strategy.CircuitBreakerStateMachine;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @Autowired
    private CircuitBreakerStateMachine stateMachine;

    private int failCount = 0;

    @GetMapping("/api")
    @CircuitBreaker(failureThreshold = 3, successThreshold = 2, timeout = 5, key = "api")
    public String api() {
        failCount++;
        if (failCount <= 3) {
            throw new RuntimeException("模拟异常");
        }
        return "OK";
    }

    @GetMapping("/state")
    public String state() {
        return stateMachine.getState("api").name();
    }
}
