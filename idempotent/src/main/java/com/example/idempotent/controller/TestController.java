package com.example.idempotent.controller;

import com.example.idempotent.annotation.Idempotent;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @PostMapping("/order")
    @Idempotent(key = "#request.getHeader('Idempotent-Key')", ttl = 1, timeUnit = java.util.concurrent.TimeUnit.HOURS)
    public String createOrder(HttpServletRequest request) {
        return "下单成功";
    }
}
