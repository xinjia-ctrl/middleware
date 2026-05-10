package com.example.ratelimit.controller;

import com.example.ratelimit.annotation.RateLimit;
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
}
