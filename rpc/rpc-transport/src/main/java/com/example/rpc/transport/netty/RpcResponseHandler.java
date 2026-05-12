package com.example.rpc.transport.netty;

import com.example.rpc.core.protocol.RpcResponse;
import com.example.rpc.transport.netty.codec.RpcMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 客户端响应处理器
 */
public class RpcResponseHandler extends SimpleChannelInboundHandler<RpcMessage> {

    private static final Logger log = LoggerFactory.getLogger(RpcResponseHandler.class);

    private final Map<Long, CompletableFuture<Object>> pendingRequests;

    public RpcResponseHandler(Map<Long, CompletableFuture<Object>> pendingRequests) {
        this.pendingRequests = pendingRequests;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) {
        CompletableFuture<Object> future = pendingRequests.remove(msg.getRequestId());
        if (future != null) {
            // 反序列化交由调用方处理
            future.complete(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("client handler error", cause);
        ctx.close();
    }
}
