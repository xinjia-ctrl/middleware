package com.example.rpc.client;

import com.example.rpc.protocol.MyDecode;
import com.example.rpc.protocol.MyEncode;
import com.example.rpc.protocol.RpcHeartbeatHandler;
import com.example.rpc.protocol.RpcRequest;
import com.example.rpc.protocol.RpcResponse;
import com.example.rpc.protocol.ObjectSerializer;
import com.example.rpc.protocol.Serializer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class NettyRpcClient implements RpcClient {

    private final EventLoopGroup group = new NioEventLoopGroup(4);
    private final Map<String, Channel> channelCache = new ConcurrentHashMap<>();
    private final Map<Long, CompletableFuture<RpcResponse>> pending = new ConcurrentHashMap<>();
    private final Map<Long, String> pendingChannels = new ConcurrentHashMap<>();
    private final AtomicLong requestIdGen = new AtomicLong(0);
    private final Serializer serializer;
    private final int timeoutSeconds;

    public NettyRpcClient() {
        this(new ObjectSerializer());
    }

    public NettyRpcClient(Serializer serializer) {
        this(serializer, 3);
    }

    public NettyRpcClient(Serializer serializer, int timeoutSeconds) {
        this.serializer = serializer;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public RpcResponse sendRequest(RpcRequest request, String host, int port) {
        String key = host + ":" + port;
        Channel channel = channelCache.compute(key, (ignored, oldChannel) -> {
            if (oldChannel != null && oldChannel.isActive()) {
                return oldChannel;
            }
            if (oldChannel != null) {
                oldChannel.close();
            }
            return createChannel(host, port);
        });

        long requestId = requestIdGen.incrementAndGet();
        request.setRequestId(requestId);

        CompletableFuture<RpcResponse> future = new CompletableFuture<>();
        pending.put(requestId, future);
        pendingChannels.put(requestId, key);

        channel.writeAndFlush(request).addListener(writeFuture -> {
            if (!writeFuture.isSuccess()) {
                CompletableFuture<RpcResponse> removed = pending.remove(requestId);
                pendingChannels.remove(requestId);
                if (removed != null) {
                    removed.completeExceptionally(writeFuture.cause());
                }
                channelCache.remove(key, channel);
                channel.close();
            }
        });

        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            pending.remove(requestId);
            pendingChannels.remove(requestId);
            throw new RuntimeException("rpc call timeout or failed", e);
        }
    }

    private Channel createChannel(String host, int port) {
        Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new MyEncode(serializer))
                                .addLast(new MyDecode())
                                .addLast(new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS))
                                .addLast(new RpcHeartbeatHandler(false))
                                .addLast(new SimpleChannelInboundHandler<RpcResponse>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse response) {
                                        CompletableFuture<RpcResponse> f =
                                                pending.remove(response.getRequestId());
                                        pendingChannels.remove(response.getRequestId());
                                        if (f != null) f.complete(response);
                                    }

                                    @Override
                                    public void channelInactive(ChannelHandlerContext ctx) {
                                        String channelKey = host + ":" + port;
                                        pendingChannels.forEach((id, key) -> {
                                            if (channelKey.equals(key)) {
                                                CompletableFuture<RpcResponse> f = pending.remove(id);
                                                pendingChannels.remove(id);
                                                if (f != null) {
                                                    f.completeExceptionally(new RuntimeException("rpc channel closed"));
                                                }
                                            }
                                        });
                                    }
                                });
                    }
                });

        try {
            return bootstrap.connect(host, port).syncUninterruptibly().channel();
        } catch (Exception e) {
            throw new RuntimeException("connect to " + host + ":" + port + " failed", e);
        }
    }

    @Override
    public void close() {
        channelCache.values().forEach(Channel::close);
        group.shutdownGracefully();
    }
}
