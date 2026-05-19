# rpc

基于 Netty + ZooKeeper 的轻量级 RPC 框架，支持自定义协议、多序列化、负载均衡、过滤器链与生产级中间件集成。

## 功能特性

- **自定义协议**：14 字节帧头（魔数 + 版本 + 消息类型 + 序列化方式 + 压缩标记 + 保留位 + 消息体长度），Gzip 条件压缩（body > 256B）
- **双传输层**：Netty NIO（异步非阻塞）与 BIO（简单阻塞），统一 `RpcClient` 接口
- **多序列化**：JDK 原生序列化与 fastjson JSON 序列化，可扩展
- **ZooKeeper 注册中心**：基于 CuratorFramework 的服务注册与发现，支持本地缓存
- **负载均衡**：随机、轮询、加权随机、一致性哈希四种策略
- **过滤器链**：限流（`RpcRateLimitFilter`，CAS 无锁限流器）、熔断（`RpcCircuitBreakerFilter`，状态机 + 滑动窗口）、幂等（`RpcIdempotentFilter`）
- **降级链**：重试耗尽后触发 `RpcFallback`，可通过 `RpcFallback.chain` 组合缓存结果与 Mock 默认值
- **线程池隔离**：IO 线程与业务线程分离，`ThreadPoolExecutor` + 快速失败背压
- **双层限流**：`Semaphore` 全局并发控制 + `AtomicLong` CAS 连接级速率限制
- **心跳保活**：基于 Netty `IdleStateHandler` 的客户端心跳，超时自动关闭
- **JDK 动态代理**：`RpcClientProxy` 透明调用，支持重试与降级

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- ZooKeeper 3.8+（注册中心）
- 依赖模块：rate-limiter、idempotent、circuit-breaker

### 安装

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>rpc</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 使用示例

```java
// 1. 服务端：注册服务并启动
String zkAddr = "127.0.0.1:2181";
int port = 8088;
ServiceProvider provider = RpcBootstrap.newServiceProvider(zkAddr, "127.0.0.1:" + port);
provider.addService(UserService.class, new UserServiceImpl());
provider.addService(OrderService.class, new OrderServiceImpl());

NettyRpcServer server = RpcBootstrap.createServer(provider);
server.start(port);

// 2. 客户端：创建代理并调用
RpcClientProxy proxy = RpcBootstrap.createClientProxy(
        RpcClientConfig.builder()
                .zkAddr(zkAddr)
                .serializer(new JsonSerializer())
                .loadBalance(RpcConstants.LB_CONSISTENT_HASH)
                .retryCount(2)
                .timeoutSeconds(3)
                .build());

UserService userService = proxy.create(UserService.class);
User user = userService.findByName("张三");

// 3. 带降级的客户端
RpcFallback fallback = (interfaceName, method, methodArgs, cause) -> {
    log.warn("调用失败，返回降级结果", cause);
    return new User("默认用户");
};
RpcClientProxy proxyWithFallback = RpcBootstrap.createClientProxy(
        RpcClientConfig.builder().zkAddr(zkAddr).build(), fallback);
```

## 自定义协议

```
 0      1      2      3      4      5      6      7      8      9      10     11     12     13
├───────┴───────┴───────┴───────┴───────┴───────┴───────┴───────┼───────┴───────┴───────┴───────┤
│        魔数          │      版本      │     消息类型    │    序列化方式   │
│      0xCCDD          │       1       │    0/1/2       │     0/1        │
├───────────────────────┼───────────────┼────────────────┴───────────────┤
│    压缩标记  │  保留位  │             消息体长度(4B)                    │
│    1=gzip    │   0x00   │                                               │
├───────────────┴──────────┴──────────────────────────────────────────────┤
│                               消息体                                     │
│                           (可变长度)                                      │
└──────────────────────────────────────────────────────────────────────────┘
```

- 消息类型：0=REQUEST，1=RESPONSE，2=HEARTBEAT
- 序列化方式：0=JDK，1=JSON
- 压缩标记：第 7 bit 置 1 表示 Gzip 压缩
- 解码时校验魔数 `0xCCDD`，不匹配直接关闭连接

## 过滤器链

| 过滤器 | 功能 | 说明 |
|--------|------|------|
| `RpcRateLimitFilter` | 限流 | 基于 CAS 无锁限流器，默认 100 rps |
| `RpcCircuitBreakerFilter` | 熔断 | 计数器状态机 + 滑动窗口两种策略 |
| `RpcIdempotentFilter` | 幂等 | 5s 内相同请求去重 |

## 架构设计

```
┌──────────────┐    ┌─────────────────┐    ┌──────────────────┐
│  RpcClient   │───▶│   Netty NIO /   │───▶│  ZooKeeper 注册   │
│  (JDK Proxy) │    │   Simple BIO    │    │  (Curator)        │
└──────┬───────┘    └────────┬────────┘    └──────────────────┘
       │                     │
       ▼                     ▼
┌──────────────┐    ┌─────────────────┐
│ 过滤器链      │    │ 负载均衡          │
│ 限流·熔断·幂等 │    │ 随机·轮询·哈希    │
└──────────────┘    └─────────────────┘

┌──────────────┐    ┌─────────────────┐
│ 降级链        │    │ 线程池隔离        │
│ 缓存→Mock    │    │ IO ↔ 业务线程     │
└──────────────┘    └─────────────────┘
```

## 项目结构

```
rpc/
├── client/        RPC 客户端（Netty + BIO + 代理 + 降级）
├── server/        RPC 服务端（Netty + 服务注册）
├── protocol/      自定义协议（编解码 + 序列化 + 心跳）
├── config/        配置、常量、引导类
├── registry/      ZooKeeper 注册与发现
├── loadbalance/   负载均衡策略
├── filter/        RPC 过滤器链（限流/熔断/幂等）
└── RpcDemo.java   启动演示
```

## 配置参数

`RpcClientConfig` 构建参数：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `serializer` | `Serializer` | `ObjectSerializer` | 序列化方式 |
| `loadBalance` | `String` | `random` | 负载均衡策略 |
| `retryCount` | `int` | `2` | 失败重试次数 |
| `timeoutSeconds` | `int` | `3` | 单次调用超时 |

## 技术栈

- Netty 4.1 + NIO
- ZooKeeper + CuratorFramework 5.7
- fastjson 2.0
- Logback
- rate-limiter / idempotent / circuit-breaker（自研中间件）

## 许可证

MIT
