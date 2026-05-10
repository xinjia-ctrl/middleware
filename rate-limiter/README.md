# rate-limiter

基于注解 + AOP 的 Spring Boot 限流中间件，开箱即用。

## 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>rate-limiter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. 在方法上添加注解

```java
@RestController
public class DemoController {

    @GetMapping("/api")
    @RateLimit(permitsPerSecond = 5)
    public String hello() {
        return "OK";
    }
}
```

## 功能特性

- TODO

## 限流策略

- TODO

## 配置说明

- TODO
