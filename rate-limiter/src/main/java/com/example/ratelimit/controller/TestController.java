package com.example.ratelimit.controller;

import com.example.ratelimit.annotation.RateLimit;
import com.example.ratelimit.strategy.RejectedStrategy;
import com.example.ratelimit.strategy.StrategyType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/token-bucket")
    @RateLimit(strategy = StrategyType.TOKEN_BUCKET, permitsPerSecond = 2, capacity = 2, key = "#request.remoteAddr")
    public String tokenBucket(HttpServletRequest request) {
        return "TOKEN_BUCKET: OK";
    }

    @GetMapping("/fixed-window")
    @RateLimit(strategy = StrategyType.FIXED_WINDOW, permits = 3, window = 10, timeUnit = java.util.concurrent.TimeUnit.SECONDS, key = "#request.remoteAddr")
    public String fixedWindow(HttpServletRequest request) {
        return "FIXED_WINDOW: OK";
    }

    @GetMapping("/sliding-window")
    @RateLimit(strategy = StrategyType.SLIDING_WINDOW, permits = 3, window = 10, timeUnit = java.util.concurrent.TimeUnit.SECONDS, key = "#request.remoteAddr")
    public String slidingWindow(HttpServletRequest request) {
        return "SLIDING_WINDOW: OK";
    }

    @GetMapping("/leaky-bucket")
    @RateLimit(strategy = StrategyType.LEAKY_BUCKET, leakRate = 1, leakCapacity = 3, key = "#request.remoteAddr")
    public String leakyBucket(HttpServletRequest request) {
        return "LEAKY_BUCKET: OK";
    }

    @GetMapping("/reject-silent")
    @RateLimit(permitsPerSecond = 1, capacity = 1, key = "#request.remoteAddr", rejectedStrategy = RejectedStrategy.SILENT)
    public String rejectSilent(HttpServletRequest request) {
        return "REJECT_SILENT: OK";
    }

    @GetMapping("/reject-fallback")
    @RateLimit(permitsPerSecond = 1, capacity = 1, key = "#request.remoteAddr", rejectedStrategy = RejectedStrategy.CALLER_RUNS, fallback = "myFallback")
    public String rejectFallback(HttpServletRequest request) {
        return "REJECT_FALLBACK: OK";
    }

    public String myFallback(HttpServletRequest request) {
        return "当前请求被限流，返回降级结果";
    }
}
