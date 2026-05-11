# circuit-breaker

基于注解 + AOP 的 Spring Boot 熔断器中间件，支持三态转换和降级策略，开箱即用。

## 功能特性

- 三态转换：CLOSED（正常）→ OPEN（熔断）→ HALF_OPEN（半开）→ CLOSED
- 失败计数熔断：连续失败达到阈值自动熔断
- 超时恢复：熔断后自动进入半开状态尝试恢复
- 三种拒绝策略：抛异常、静默拒绝、Fallback 降级
- Key 隔离：支持 SpEL 表达式按接口 / 用户维度独立熔断
- 半开限流：半开状态下只放行有限请求，避免雪崩
- Spring Boot 自动装配

## 状态说明

```
CLOSED (正常)
  │  failureCount >= failureThreshold
  │  ───────────────────────────► OPEN (熔断)
  │                               │
  │                               │  timeout 后 → HALF_OPEN (半开)
  │                               │  放行 successThreshold 个请求
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

    // 基础熔断：3次失败后熔断，5秒后尝试恢复，2次成功则关闭
    @GetMapping("/api")
    @CircuitBreaker(failureThreshold = 3, successThreshold = 2, timeout = 5)
    public String api() {
        // 调用外部服务
        return restTemplate.getForObject("http://external/api", String.class);
    }

    // 熔断时走 Fallback 降级
    @GetMapping("/api2")
    @CircuitBreaker(failureThreshold = 3, fallback = "myFallback")
    public String api2() {
        return remoteCall();
    }
    public String myFallback() {
        return "服务繁忙，返回缓存数据";
    }

    // 熔断时静默返回 null（不抛异常）
    @GetMapping("/api3")
    @CircuitBreaker(failureThreshold = 3, rejectedStrategy = RejectedStrategy.SILENT)
    public String api3() {
        return remoteCall();
    }
}
```

## 配置说明

### @CircuitBreaker 注解参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `failureThreshold` | `int` | `5` | 触发熔断的失败次数阈值 |
| `successThreshold` | `int` | `3` | 半开状态下关闭所需的连续成功次数 |
| `timeout` | `long` | `10` | 熔断后进入半开的等待时间 |
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
├── strategy/      熔断状态机和拒绝策略
├── config/        自动配置和异常处理
├── exception/     熔断异常
├── controller/    测试接口
└── util/          SpEL Key 解析
```

## 技术栈

- Spring Boot 3.4.3
- Spring AOP
- Spring Data Redis + Lettuce（可选）
- Lua（可选）

## 许可证

MIT
