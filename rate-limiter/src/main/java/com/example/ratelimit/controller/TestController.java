package com.example.ratelimit.controller;

import com.example.ratelimit.annotation.RateLimit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/hello")
    @RateLimit(permitsPerSecond = 2, capacity = 2)
    public String hello() {
        return "OK";
    }
}
