package com.example.rpc.transport.netty;

import com.example.rpc.core.protocol.RpcRequest;
import com.example.rpc.core.serialization.Serializer;
import com.example.rpc.transport.RpcClient;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 Netty 的 RPC 客户端
 */
public class NettyRpcClient implements RpcClient {

    private static final Logger log = LoggerFactory.getLogger(NettyRpcClient.class);

    private final Serializer serializer;
    private final EventLoopGroup group = new NioEventLoopGroup();
    private final Map<String, Channel> channelCache = new ConcurrentHashMap<>();
    private final Map<Long, CompletableFuture<Object>> pendingRequests = new ConcurrentHashMap<>();

    private Channel channel;

    public NettyRpcClient(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public void connect(String host, int port) {
        String key = host + ":" + port;
        if (channelCache.containsKey(key)) {
            this.channel = channelCache.get(key);
            return;
        }

        Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline()
                                .addLast(new RpcMessageEncoder(serializer))
                                .addLast(new RpcMessageDecoder())
                                .addLast(new RpcResponseHandler(pendingRequests));
                    }
                })
                .option(ChannelOption.TCP_NODELAY, true);

        try {
            ChannelFuture future = bootstrap.connect(host, port).syncUninterruptibly();
            this.channel = future.channel();
            channelCache.put(key, this.channel);
            log.info("NettyRpcClient connected to {}:{}", host, port);
        } catch (Exception e) {
            log.error("NettyRpcClient connect failed to {}:{}", host, port, e);
            throw new RuntimeException("connect failed", e);
        }
    }

    @Override
    public boolean isConnected() {
        return channel != null && channel.isActive();
    }

    @Override
    public void close() {
        for (Channel ch : channelCache.values()) {
            ch.close();
        }
        group.shutdownGracefully();
        log.info("NettyRpcClient shut down");
    }

    public Channel getChannel() {
        return channel;
    }

    public Map<Long, CompletableFuture<Object>> getPendingRequests() {
        return pendingRequests;
    }
}
