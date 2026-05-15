# idempotent

基于注解 + AOP 的 Spring Boot 幂等组件，防止重复提交，支持结果缓存和分布式部署。

## 功能特性

- 注解驱动：`@Idempotent` 声明式幂等控制
- 结果缓存：重复请求直接返回首次执行结果，而非抛异常
- 双重存储：自动检测 Redis 依赖，有 Redis 走分布式去重，否则降级为本地内存
- CAS 锁竞争：基于 `replace` 原子操作的乐观锁，避免重复提交并发穿透
- SpEL 表达式：灵活提取幂等 Key（请求头、参数、用户 ID 等）
- 定期过期清理：本地模式下每 10 次操作触发过期 key 清理
- Spring Boot 自动装配，零配置即可使用

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- Redis 7+（分布式存储需要）

### 安装

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>idempotent</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 使用示例

```java
@RestController
public class ApiController {

    // 基础用法：请求头携带幂等 Key，24 小时内自动去重
    @PostMapping("/order")
    @Idempotent(key = "#request.getHeader('Idempotent-Key')")
    public String createOrder(HttpServletRequest request) {
        return "下单成功";
    }

    // 自定义 TTL：1 小时内不允许重复提交
    @PostMapping("/payment")
    @Idempotent(key = "#request.getHeader('Idempotent-Key')", ttl = 1, timeUnit = TimeUnit.HOURS)
    public String pay(HttpServletRequest request) {
        return "支付成功";
    }

    // 结果缓存：重复请求直接返回上次的成功结果，不再执行方法体
    @PostMapping("/submit")
    @Idempotent(key = "#request.getHeader('Idempotent-Key')", ttl = 2,
                timeUnit = TimeUnit.HOURS, cacheResult = true)
    public String submit(HttpServletRequest request) {
        return "提交成功";
    }

    // SpEL：按用户 ID + 请求参数组合去重
    @PostMapping("/apply")
    @Idempotent(key = "#userId + ':' + #request.getParameter('type')")
    public String apply(String userId, HttpServletRequest request) {
        return "申请成功";
    }
}
```

### 验证重复请求

```bash
# 第一次请求 — 成功
curl -X POST http://localhost:8082/order \
  -H "Idempotent-Key: order-123"

# 重复请求 — 返回 409
curl -X POST http://localhost:8082/order \
  -H "Idempotent-Key: order-123"
# {"code":409,"message":"重复请求，请勿重复提交"}

# cacheResult=true 时重复请求直接返回缓存结果
curl -X POST http://localhost:8082/submit \
  -H "Idempotent-Key: submit-456"
# "提交成功"（第二次直接返回，不执行业务方法）
```

## 配置说明

### @Idempotent 注解参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `key` | `String` | `""` | 幂等 Key，支持 SpEL |
| `ttl` | `long` | `24` | 幂等 Key 有效期 |
| `timeUnit` | `TimeUnit` | `HOURS` | 有效期单位 |
| `message` | `String` | `重复请求，请勿重复提交` | 重复请求提示信息 |
| `cacheResult` | `boolean` | `false` | 是否缓存执行结果 |

### Key 提取规则

SpEL 表达式中通过 `#参数名` 访问方法参数，支持调用对象方法：

- `#request.getHeader('Idempotent-Key')` — 从请求头提取
- `#request.remoteAddr` — 按 IP 去重
- `#userId` — 按参数值去重
- `#dto.orderNo` — 按对象属性去重

不配置 key 时，默认以方法签名作为 Key。

### 存储策略

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

| 存储 | 原理 | 适用场景 |
|------|------|----------|
| `InMemoryIdempotentStorage` | `ConcurrentHashMap` + CAS replace | 单实例，开发/测试 |
| `RedisIdempotentStorage` | `SET NX PX` + 类型安全序列化 | 多实例生产部署 |

### 结果缓存流程

```
首次请求:  trySave OK → 执行业务 → saveResult(key, 结果)
重复请求:  trySave 失败 → getResult(key) → 有缓存 → 直接返回
                                          → 无缓存 → 抛异常
业务异常:  remove(key) 同时清理锁和缓存，允许重试
```

## 项目结构

```
idempotent/
├── annotation/   @Idempotent 注解
├── aspect/       AOP 切面
├── strategy/     存储策略（内存 / Redis）
├── config/       自动配置和异常处理
├── exception/    重复请求异常
├── util/         SpEL Key 解析
└── controller/   测试接口
```

## 技术栈

- Spring Boot 3.4.3 + Spring AOP
- Spring Data Redis + Lettuce
- Jackson

## 许可证

MIT
