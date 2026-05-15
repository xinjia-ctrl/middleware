# circuit-breaker

基于注解 + AOP 的 Spring Boot 熔断器中间件，支持三态状态机与滑动窗口两种熔断策略。

## 功能特性

- **双熔断策略**：计数器状态机（CLOSED→OPEN→HALF_OPEN）和滑动窗口（10s 窗口，按错误比例）
- **三态转换**：CLOSED（正常）→ OPEN（熔断）→ HALF_OPEN（半开探活）→ CLOSED
- **半开限流**：半开状态下只放行有限探活请求，防止雪崩
- **半开超时逃生**：探活请求超时未完成时自动回到 OPEN，防止永久卡死
- **三种拒绝策略**：抛异常、静默拒绝、Fallback 降级
- **Key 隔离**：支持 SpEL 表达式按接口/用户维度独立熔断
- Spring Boot 自动装配

## 状态机

```
CLOSED (正常)
  │  failureCount >= failureThreshold
  │  ───────────────────────────► OPEN (熔断)
  │                               │
  │                               │  timeout 后 → HALF_OPEN (半开)
  │                               │  放行 successThreshold 个探活请求
  │                               │
  │  successCount >= successThreshold
  │  ◄────────────────────────────┘
  │  全部成功 → CLOSED
  │  任一失败 → OPEN
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+

### 安装

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>circuit-breaker</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 使用示例

```java
@RestController
public class DemoController {

    // 计数器熔断：3次失败后熔断，5秒后半开，2次连续成功则恢复
    @GetMapping("/api")
    @CircuitBreaker(failureThreshold = 3, successThreshold = 2, timeout = 5)
    public String api() {
        return restTemplate.getForObject("http://external/api", String.class);
    }

    // 熔断时走 Fallback 降级（CALLER_RUNS）
    @GetMapping("/api2")
    @CircuitBreaker(failureThreshold = 3, rejectedStrategy = CALLER_RUNS, fallback = "myFallback")
    public String api2() { return remoteCall(); }
    public String myFallback() { return "服务繁忙，返回缓存数据"; }

    // 滑动窗口熔断：10s内失败比例超 50% 且请求数超 10 则熔断
    @GetMapping("/api3")
    @CircuitBreaker(failureThreshold = 10, message = "服务异常")
    public String api3() { return remoteCall(); }

    // 熔断时静默返回 null
    @GetMapping("/api4")
    @CircuitBreaker(failureThreshold = 3, rejectedStrategy = SILENT)
    public String api4() { return remoteCall(); }
}
```

## 配置说明

### @CircuitBreaker 注解参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `failureThreshold` | `int` | `5` | 触发熔断的失败次数/最小请求数 |
| `successThreshold` | `int` | `3` | 半开状态下关闭所需的连续成功次数 |
| `timeout` | `long` | `10` | 熔断持续时间 |
| `timeUnit` | `TimeUnit` | `SECONDS` | 超时时间单位 |
| `rejectedStrategy` | `RejectedStrategy` | `ABORT` | 熔断时的拒绝策略 |
| `fallback` | `String` | `""` | 降级方法名 |
| `key` | `String` | `""` | 熔断 Key，支持 SpEL |
| `message` | `String` | `Circuit breaker is open...` | 熔断提示信息 |

### 拒绝策略

| 策略 | 熔断时 | 方法异常时 |
|------|--------|------------|
| `ABORT` | 抛 503 异常 | 记录失败，抛原始异常 |
| `SILENT` | 返回 null | 记录失败，返回 null |
| `CALLER_RUNS` | 调用 fallback | 记录失败，调用 fallback |

## 项目结构

```
circuit-breaker/
├── annotation/    @CircuitBreaker 注解
├── aspect/        AOP 切面
├── strategy/      状态机 + 滑动窗口 + 拒绝策略
├── config/        自动配置和异常处理
├── exception/     熔断异常
├── util/          SpEL Key 解析
└── controller/    测试接口
```

## 技术栈

- Spring Boot 3.4.3 + Spring AOP
- SpEL

## 许可证

MIT
