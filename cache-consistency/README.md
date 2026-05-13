# cache-consistency

基于注解 + AOP 的 Spring Boot 缓存一致性中间件，解决数据库与缓存（Redis）之间的数据一致性问题，开箱即用。

## 功能特性

- 两种一致性策略：**先删缓存后更新**（EVICT_AFTER）和**延迟双删**（DOUBLE_DELETE）
- 两种缓存操作：淘汰缓存（EVICT）和写入缓存（PUT）
- PUT 模式支持 JSON 序列化，自动缓存方法返回值
- 存储降级：自动检测 Redis 依赖，无 Redis 时降级为本地内存
- SpEL 表达式动态提取缓存 Key
- Spring Boot 自动装配，零配置即可使用

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- Redis（推荐，本地内存模式不需要）

### 安装

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>cache-consistency</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 使用示例

```java
@RestController
public class UserController {

    // 先删缓存，再更新数据库
    @GetMapping("/user/update")
    @CacheConsistent(key = "'user:' + #id", action = CacheAction.EVICT,
            strategy = CacheConsistentStrategy.EVICT_AFTER)
    public String updateUser(@RequestParam Long id, @RequestParam String name) {
        return "用户 " + id + " 更新成功";
    }

    // PUT 策略：更新数据库后，将方法返回值写入缓存
    @GetMapping("/order/create")
    @CacheConsistent(key = "'order:' + #orderId", action = CacheAction.PUT,
            strategy = CacheConsistentStrategy.EVICT_AFTER,
            ttl = 2, timeUnit = TimeUnit.HOURS)
    public String createOrder(@RequestParam String orderId) {
        return "{\"orderId\":\"" + orderId + "\",\"status\":\"PAID\"}";
    }

    // 延迟双删：解决主从复制场景下的缓存一致性问题
    @GetMapping("/inventory/update")
    @CacheConsistent(key = "'inventory:' + #productId", action = CacheAction.EVICT,
            strategy = CacheConsistentStrategy.DOUBLE_DELETE, delayMillis = 300)
    public String updateInventory(@RequestParam Long productId, @RequestParam Integer stock) {
        return "库存 " + productId + " 更新为 " + stock;
    }
}
```

## 配置说明

### @CacheConsistent 注解参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `key` | `String` | — | 缓存 Key，支持 SpEL（必填） |
| `action` | `CacheAction` | `EVICT` | 缓存操作类型 |
| `strategy` | `CacheConsistentStrategy` | `EVICT_AFTER` | 一致性策略 |
| `result` | `String` | `""` | 写入缓存的表达式（PUT 时使用，默认取返回值） |
| `ttl` | `long` | `1` | 缓存 TTL（PUT 时有效） |
| `timeUnit` | `TimeUnit` | `HOURS` | TTL 时间单位 |
| `delayMillis` | `long` | `500` | 延迟双删的等待时间（毫秒） |

### 一致性策略

| 策略 | 流程 | 适用场景 |
|------|------|----------|
| `EVICT_AFTER` | 执行方法 → 删除缓存 | 读写分离延迟低，简单场景 |
| `DOUBLE_DELETE` | 删缓存 → 执行方法 → 延迟删缓存 | 主从复制有延迟，高并发读写 |

### 存储策略

引入 `spring-boot-starter-data-redis` 且配置 Redis 后自动启用 Redis 存储，否则降级到本地内存：

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

也可通过 `application.yml` 自定义配置：

```yaml
cache-consistency:
  key-prefix: "cache:"          # 缓存 Key 前缀
  default-delay-millis: 300      # 延迟双删默认等待时间
```

## 项目结构

```
cache-consistency/
├── annotation/     @CacheConsistent 注解
├── aspect/         AOP 切面（拦截 + 策略执行）
├── strategy/       缓存策略（EVICT / PUT + 延迟双删）
│   ├── CacheAction.java            缓存操作枚举
│   ├── CacheConsistentStrategy.java 一致性策略枚举
│   ├── CacheStorage.java           存储接口
│   ├── RedisCacheStorage.java       Redis 实现
│   └── InMemoryCacheStorage.java    本地内存实现
├── config/         自动配置和配置属性
└── util/           SpEL Key 解析
```

## 技术栈

- Spring Boot 3.4.3
- Spring AOP + SpEL
- Spring Data Redis + Lettuce
- Jackson

## 许可证

MIT
