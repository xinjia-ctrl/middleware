package com.example.rpc.transport.netty;

import com.example.rpc.core.protocol.RpcRequest;
import com.example.rpc.core.protocol.RpcResponse;
import com.example.rpc.core.serialization.Serializer;
import com.example.rpc.transport.netty.codec.RpcMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 服务端请求处理器
 */
@ChannelHandler.Sharable
public class RpcRequestHandler extends SimpleChannelInboundHandler<RpcMessage> {

    private static final Logger log = LoggerFactory.getLogger(RpcRequestHandler.class);

    private final Serializer serializer;
    private final Map<String, Object> serviceBeanMap;

    public RpcRequestHandler(Serializer serializer, Map<String, Object> serviceBeanMap) {
        this.serializer = serializer;
        this.serviceBeanMap = serviceBeanMap;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) {
        // 解码请求体
        RpcRequest request = serializer.deserialize(msg.getBody(), RpcRequest.class);

        RpcResponse response;
        try {
            Object bean = serviceBeanMap.get(request.getServiceName());
            if (bean == null) {
                response = new RpcResponse(new RuntimeException("service not found: " + request.getServiceName()));
            } else {
                Method method = bean.getClass().getMethod(request.getMethodName(), request.getParameterTypes());
                Object result = method.invoke(bean, request.getParameters());
                response = new RpcResponse(result);
            }
        } catch (Exception e) {
            log.error("invoke failed: {}.{}", request.getServiceName(), request.getMethodName(), e);
            response = new RpcResponse(e);
        }

        // 编码并回写响应
        byte[] body = serializer.serialize(response);
        RpcMessage responseMsg = new RpcMessage(
                com.example.rpc.core.protocol.MessageType.RESPONSE,
                serializer.getType(),
                msg.getRequestId(),
                body);
        ctx.writeAndFlush(responseMsg);
    }
}
