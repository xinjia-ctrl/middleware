package com.example.rpc.client;

import com.example.rpc.core.protocol.RpcRequest;
import com.example.rpc.core.protocol.RpcResponse;
import com.example.rpc.core.serialization.JsonSerializer;
import com.example.rpc.core.serialization.Serializer;
import com.example.rpc.registry.RegistryService;
import com.example.rpc.registry.ServiceMeta;
import com.example.rpc.transport.netty.NettyRpcClient;
import com.example.rpc.transport.netty.RpcMessageEncoder;
import com.example.rpc.transport.netty.codec.RpcMessage;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * RPC 客户端动态代理工厂
 *
 * 为服务接口生成代理，调用时自动完成：序列化 -> 传输 -> 反序列化
 */
public class RpcClientProxy {

    private final RegistryService registryService;
    private final Serializer serializer;
    private final long timeoutMs;

    public RpcClientProxy(RegistryService registryService) {
        this(registryService, new JsonSerializer(), 3000);
    }

    public RpcClientProxy(RegistryService registryService, Serializer serializer, long timeoutMs) {
        this.registryService = registryService;
        this.serializer = serializer;
        this.timeoutMs = timeoutMs;
    }

    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> serviceInterface) {
        return (T) Proxy.newProxyInstance(
                serviceInterface.getClassLoader(),
                new Class<?>[]{serviceInterface},
                new RpcInvocationHandler(serviceInterface));
    }

    private class RpcInvocationHandler implements InvocationHandler {

        private final Class<?> serviceInterface;

        RpcInvocationHandler(Class<?> serviceInterface) {
            this.serviceInterface = serviceInterface;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 跳过 Object 方法
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }

            String serviceName = serviceInterface.getName();
            RpcRequest request = new RpcRequest(
                    serviceName, method.getName(),
                    method.getParameterTypes(), args);

            // 发现服务
            java.util.List<ServiceMeta> services = registryService.discover(serviceName);
            if (services.isEmpty()) {
                throw new RuntimeException("no service found: " + serviceName);
            }
            // 简单取第一个（后续由负载均衡决定）
            ServiceMeta meta = services.get(0);

            // 连接并发送请求
            NettyRpcClient client = new NettyRpcClient(serializer);
            client.connect(meta.getHost(), meta.getPort());

            long requestId = RpcMessageEncoder.requestId();
            CompletableFuture<Object> future = new CompletableFuture<>();
            client.getPendingRequests().put(requestId, future);

            byte[] body = serializer.serialize(request);
            RpcMessage message = new RpcMessage(
                    com.example.rpc.core.protocol.MessageType.REQUEST,
                    serializer.getType(), requestId, body);
            client.getChannel().writeAndFlush(message);

            // 等待响应
            Object raw = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            if (raw instanceof RpcMessage respMsg) {
                RpcResponse response = serializer.deserialize(respMsg.getBody(), RpcResponse.class);
                if (!response.isSuccess()) {
                    throw new RuntimeException("rpc call failed", response.getException());
                }
                return response.getResult();
            }
            throw new RuntimeException("unexpected response type: " + raw.getClass());
        }
    }
}
