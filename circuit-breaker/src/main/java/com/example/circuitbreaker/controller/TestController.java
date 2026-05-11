package com.example.circuitbreaker.controller;

import com.example.circuitbreaker.annotation.CircuitBreaker;
import com.example.circuitbreaker.strategy.CircuitBreakerStateMachine;
import com.example.circuitbreaker.strategy.RejectedStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class TestController {

    @Autowired
    private CircuitBreakerStateMachine stateMachine;

    private final AtomicInteger apiFailCount = new AtomicInteger(0);
    private final AtomicInteger api2FailCount = new AtomicInteger(0);

    @GetMapping("/api")
    @CircuitBreaker(failureThreshold = 3, successThreshold = 2, timeout = 5, key = "api")
    public String api() {
        apiFailCount.incrementAndGet();
        throw new RuntimeException("模拟异常");
    }

    @GetMapping("/api2")
    @CircuitBreaker(failureThreshold = 3, successThreshold = 2, timeout = 5,
            key = "api2", fallback = "apiFallback")
    public String api2() {
        api2FailCount.incrementAndGet();
        throw new RuntimeException("模拟异常");
    }

    public String apiFallback() {
        return "服务繁忙，返回降级数据";
    }

    @GetMapping("/api3")
    @CircuitBreaker(failureThreshold = 3, successThreshold = 2, timeout = 5,
            key = "api3", rejectedStrategy = RejectedStrategy.SILENT)
    public String api3() {
        throw new RuntimeException("模拟异常");
    }

    @GetMapping("/state")
    public String state(@RequestParam(defaultValue = "api") String key) {
        return stateMachine.getState(key).name();
    }
}
