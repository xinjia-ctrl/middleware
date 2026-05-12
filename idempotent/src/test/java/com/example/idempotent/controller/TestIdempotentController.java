package com.example.idempotent.controller;

import com.example.idempotent.annotation.Idempotent;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
public class TestIdempotentController {

    private int callCount = 0;

    @GetMapping("/test/dedup")
    @Idempotent(key = "#request.getHeader('Idempotent-Key')", ttl = 1, timeUnit = TimeUnit.MINUTES)
    public String dedup(HttpServletRequest request) {
        return "OK";
    }

    @GetMapping("/test/cache-result")
    @Idempotent(key = "#request.getHeader('Idempotent-Key')", ttl = 1, timeUnit = TimeUnit.MINUTES, cacheResult = true)
    public String cacheResult(HttpServletRequest request) {
        return "第一次结果";
    }

    @GetMapping("/test/retry-on-error")
    @Idempotent(key = "#request.getHeader('Idempotent-Key')", ttl = 1, timeUnit = TimeUnit.MINUTES)
    public String retryOnError(HttpServletRequest request) {
        callCount++;
        if (callCount == 1) {
            throw new RuntimeException("模拟业务异常");
        }
        return "重试成功";
    }
}
