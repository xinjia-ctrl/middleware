# rpc

基于 Netty 实现的轻量级 RPC 框架，支持自定义协议、多种序列化方式、服务注册发现与 Spring Boot 集成。

## 功能特性

- **自定义通信协议**：魔数 + 版本 + 消息类型 + 序列化方式 + 请求 ID + 体长 + 体，共 17 字节头部
- **双序列化支持**：JSON（可读性高）和 JDK 原生（性能优先）
- **多种注册中心**：本地内存注册中心和 Redis 注册中心，支持服务自动发现
- **负载均衡**：随机和轮询两种策略
- **Spring Boot 自动装配**：通过 `@EnableRpc` 开启，`@RpcService` / `@RpcReference` 声明服务
- **Netty 通信**：基于 Netty 的 NIO 长连接，异步请求响应模型

## 模块说明

| 模块 | 说明 |
|------|------|
| `rpc-core` | 公共协议定义：RpcRequest/RpcResponse、序列化接口、异常 |
| `rpc-transport` | 网络传输层：Netty 服务端/客户端、编解码器、消息处理器 |
| `rpc-registry` | 注册中心：接口抽象、本地内存实现、Redis 实现 |
| `rpc-client` | 客户端代理：JDK 动态代理、负载均衡 |
| `rpc-server` | 服务端：`@RpcService` 注解、服务注册与暴露 |
| `rpc-spring-boot-starter` | Spring Boot 集成：自动装配、Bean 后置处理器、配置属性 |

## 通信协议

```
┌─────────┬──────────┬─────────┬──────────┬──────────┬───────────┐
│ magic   │ version  │ msgType │ serial   │ requestId│ bodyLen   │
│ (2 byte)│ (1 byte) │ (1 byte)│ (1 byte) │ (8 byte) │ (4 byte)  │
├─────────┴──────────┴─────────┴──────────┴──────────┴───────────┤
│                        body (variable)                         │
└────────────────────────────────────────────────────────────────┘
```

- 魔数 `0xE5E5`：快速校验非法请求
- 消息类型：请求 / 响应 / 心跳

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- Redis（使用 Redis 注册中心时需要）

### 安装

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>rpc-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 服务端

```java
// 1. 定义接口
public interface HelloService {
    String sayHello(String name);
}

// 2. 实现并注册服务
@RpcService
public class HelloServiceImpl implements HelloService {
    @Override
    public String sayHello(String name) {
        return "Hello, " + name;
    }
}

// 3. 启动类启用 RPC
@SpringBootApplication
@EnableRpc(serverPort = 9090)
public class ServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }
}
```

### 客户端

```java
// 1. 注入远程服务
@Service
public class HelloClient {

    @RpcReference
    private HelloService helloService;

    public void greet() {
        String result = helloService.sayHello("World");
        System.out.println(result); // Hello, World
    }
}

// 2. 启动类启用 RPC
@SpringBootApplication
@EnableRpc
public class ClientApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }
}
```

## 配置说明

```yaml
rpc:
  registry: local              # 注册中心：local / redis
  server-host: 127.0.0.1       # 本机地址
  server-port: 9090            # 服务端端口
  redis:
    host: 127.0.0.1            # Redis 地址
    port: 6379                 # Redis 端口
```

## 技术栈

- Netty 4
- Spring Boot 3.4.3
- Spring AOP
- Spring Data Redis + Lettuce
- Jackson

## 许可证

MIT
