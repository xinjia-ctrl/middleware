package com.example.cacheconsistency.controller;

import com.example.cacheconsistency.annotation.CacheConsistent;
import com.example.cacheconsistency.strategy.CacheAction;
import com.example.cacheconsistency.strategy.CacheConsistentStrategy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
public class TestController {

    @GetMapping("/user/update")
    @CacheConsistent(key = "'user:' + #id", action = CacheAction.EVICT,
            strategy = CacheConsistentStrategy.EVICT_AFTER)
    public String updateUser(@RequestParam Long id, @RequestParam String name) {
        return "用户 " + id + " 更新成功";
    }

    @GetMapping("/order/create")
    @CacheConsistent(key = "'order:' + #orderId", action = CacheAction.PUT,
            strategy = CacheConsistentStrategy.EVICT_AFTER, ttl = 2, timeUnit = TimeUnit.HOURS)
    public String createOrder(@RequestParam String orderId) {
        return "{\"orderId\":\"" + orderId + "\",\"status\":\"PAID\"}";
    }

    @GetMapping("/inventory/update")
    @CacheConsistent(key = "'inventory:' + #productId", action = CacheAction.EVICT,
            strategy = CacheConsistentStrategy.DOUBLE_DELETE, delayMillis = 300)
    public String updateInventory(@RequestParam Long productId, @RequestParam Integer stock) {
        return "库存 " + productId + " 更新为 " + stock;
    }
}
