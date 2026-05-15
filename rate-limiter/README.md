# rate-limiter

基于注解 + AOP 的 Spring Boot 限流中间件，支持单机内存和分布式 Redis 两种模式。

## 功能特性

- 四种限流算法：令牌桶、固定窗口、滑动窗口、漏桶
- 单机（LOCAL）和分布式 Redis（DISTRIBUTED）双模式
- 基于 Lua 脚本保证 Redis 限流原子性
- SpEL 表达式动态提取限流 Key（IP、用户 ID、请求参数等）
- 三种拒绝策略：抛异常（ABORT）、静默拒绝（SILENT）、Fallback 降级（CALLER_RUNS）
- CAS 无锁限流器（LockFreeRateLimiter），零阻塞
- Spring Boot 自动装配，零配置即可使用

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- Redis 7+（仅 DISTRIBUTED 模式需要）

### 安装

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>rate-limiter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 使用示例

```java
@RestController
public class DemoController {

    // 单机令牌桶：每秒 2 个令牌
    @GetMapping("/api")
    @RateLimit(permitsPerSecond = 2, capacity = 2)
    public String api() { return "OK"; }

    // 分布式限流：多实例共享
    @GetMapping("/shared")
    @RateLimit(strategy = TOKEN_BUCKET, mode = DISTRIBUTED, permitsPerSecond = 5, capacity = 5)
    public String shared() { return "OK"; }

    // 按 IP 独立限流
    @GetMapping("/per-ip")
    @RateLimit(permitsPerSecond = 3, key = "#request.remoteAddr")
    public String perIp(HttpServletRequest request) { return "OK"; }

    // 限流时走 Fallback 降级
    @GetMapping("/fallback")
    @RateLimit(permitsPerSecond = 1, rejectedStrategy = CALLER_RUNS, fallback = "myFallback")
    public String fallbackDemo() { return "正常响应"; }
    public String myFallback() { return "当前请求量过大，请稍后重试"; }
}
```

### 分布式模式配置

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

## 配置说明

### @RateLimit 注解参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `strategy` | `StrategyType` | `TOKEN_BUCKET` | 限流算法 |
| `mode` | `RateLimitMode` | `LOCAL` | 本地或分布式 |
| `permits` | `long` | `100` | 窗口内最大请求数（窗口策略） |
| `window` | `long` | `1` | 时间窗口大小 |
| `timeUnit` | `TimeUnit` | `SECONDS` | 时间窗口单位 |
| `permitsPerSecond` | `double` | `10.0` | 令牌生成速率 |
| `capacity` | `int` | `100` | 桶最大容量 |
| `leakRate` | `double` | `10.0` | 漏桶出水速率 |
| `leakCapacity` | `int` | `100` | 漏桶最大容量 |
| `key` | `String` | `""` | 限流 Key，支持 SpEL |
| `rejectedStrategy` | `RejectedStrategy` | `ABORT` | 拒绝策略 |
| `fallback` | `String` | `""` | 降级方法名 |
| `message` | `String` | `Too many requests...` | 限流提示信息 |

### 拒绝策略

| 策略 | 行为 |
|------|------|
| `ABORT` | 抛 `RateLimitException`，返回 429 |
| `SILENT` | 返回 `null`，不抛异常 |
| `CALLER_RUNS` | 调用 `fallback` 指定方法降级 |

### 限流算法

| 算法 | 特点 | 适用场景 |
|------|------|----------|
| 令牌桶 | 支持突发流量，平滑限流 | API 限流 |
| 固定窗口 | 实现简单，窗口边界有毛刺 | 简单速率限制 |
| 滑动窗口 | 精度高，无边界毛刺 | 对精度要求高的场景 |
| 漏桶 | 恒定输出速率 | 流量整形，保护下游 |

## 项目结构

```
rate-limiter/
├── annotation/   @RateLimit 注解
├── aspect/       AOP 切面
├── strategy/     限流策略（本地 + Redis 实现）
│   └── redis/    Redis + Lua 分布式策略
├── config/       自动配置和异常处理
├── exception/    限流异常
├── util/         SpEL Key 解析
└── controller/   测试接口
```

## 技术栈

- Spring Boot 3.4.3 + Spring AOP
- Spring Data Redis + Lettuce
- Lua

## 许可证

MIT
