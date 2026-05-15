# middleware

基于 Spring Boot 的生产级 Java 中间件集合，涵盖限流、熔断、幂等、缓存一致性和 RPC 框架，各组件既可独立使用也可协同集成。

## 模块概览

| 模块 | 说明 | 核心依赖 |
|------|------|----------|
| [rate-limiter](./rate-limiter) | 限流中间件：令牌桶 / 固定窗口 / 滑动窗口 / 漏桶，支持本地与分布式 Redis | Spring AOP + Redis |
| [circuit-breaker](./circuit-breaker) | 熔断中间件：三态状态机 + 滑动窗口，支持半开探活与 Fallback 降级 | Spring AOP |
| [idempotent](./idempotent) | 幂等组件：基于 CAS 去重 + 结果缓存，支持本地与分布式存储 | Spring AOP + Redis |
| [cache-consistency](./cache-consistency) | 缓存一致性组件：先更新后删缓存 / 延迟双删，PUT 模式自动缓存 | Spring AOP + Redis |
| [rpc](./rpc) | 自研 RPC 框架：Netty NIO + ZooKeeper + 自定义协议，集成限流/熔断/幂等过滤器 | Netty + Curator |

## 架构设计

```
┌────────────────────────────────────────────────────────────┐
│                       Middleware                           │
├────────────┬───────────┬──────────┬───────────┬───────────┤
│ RateLimit  │  Breaker  │Idempotent│ Consistency│    RPC    │
│            │           │          │            │           │
│ 算法引擎    │ 状态机     │ CAS 去重 │ 延迟双删    │ 自定义协议 │
│ Redis Lua  │ 滑动窗口   │ 结果缓存 │ EVICT/PUT  │ NIO + ZK  │
│ 本地/分布式 │ 半开探活   │ 本地/分布 │ 存储降级    │ 过滤器链   │
└────────────┴───────────┴──────────┴───────────┴───────────┘
         │           │          │                        │
         └───────────┴──────────┴────────────────────────┘
                    核心算法层 → RPC 过滤器复用
```

- 各组件基于 **Spring Boot AOP + 注解**，零配置即可使用
- 限流、熔断、幂等的核心算法被 RPC 框架的过滤器链直接复用，实现分层治理
- 所有组件均支持存储降级：有 Redis 走分布式，无 Redis 自动降级为本地内存

## 快速开始

```xml
<parent>
    <groupId>com.example</groupId>
    <artifactId>middleware</artifactId>
    <version>1.0-SNAPSHOT</version>
</parent>
```

各模块独立可用，引入对应依赖即可。详见各模块 README：

- [限流 rate-limiter](./rate-limiter/README.md)
- [熔断 circuit-breaker](./circuit-breaker/README.md)
- [幂等 idempotent](./idempotent/README.md)
- [缓存一致性 cache-consistency](./cache-consistency/README.md)
- [RPC 框架 rpc](./rpc/README.md)

## 技术栈

- Spring Boot 3.4.3 + Spring AOP
- Netty 4.1（RPC 通信）
- ZooKeeper + CuratorFramework（RPC 注册中心）
- Spring Data Redis + Lettuce（分布式存储）
- fastjson 2.0 / JDK 序列化
- Logback

## 项目结构

```
middleware/
├── rate-limiter/         限流中间件
│   ├── annotation/       @RateLimit 注解
│   ├── aspect/           AOP 切面
│   └── strategy/         限流策略（本地 + Redis Lua）
├── circuit-breaker/      熔断中间件
│   ├── annotation/       @CircuitBreaker 注解
│   ├── aspect/           AOP 切面
│   └── strategy/         状态机 + 滑动窗口
├── idempotent/           幂等组件
│   ├── annotation/       @Idempotent 注解
│   ├── aspect/           AOP 切面
│   └── strategy/         CAS 去重 + 结果缓存
├── cache-consistency/    缓存一致性组件
│   ├── annotation/       @CacheConsistent 注解
│   ├── aspect/           AOP 切面
│   └── strategy/         EVICT/PUT + 延迟双删
└── rpc/                  自研 RPC 框架
    ├── client/           Netty/BIO 客户端 + 代理 + 降级
    ├── server/           Netty 服务端 + 线程池隔离
    ├── protocol/         自定义协议编解码 + 序列化 + 心跳
    ├── registry/         ZooKeeper 注册发现
    └── filter/           限流/熔断/幂等过滤器
```

## 许可证

MIT
