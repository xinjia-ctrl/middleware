package com.example.rpc;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class NettyRpcClient implements RpcClient {

    private final EventLoopGroup group = new NioEventLoopGroup(4);
    private final Map<String, Channel> channelCache = new ConcurrentHashMap<>();
    private final Map<Long, CompletableFuture<RpcResponse>> pending = new ConcurrentHashMap<>();
    private final AtomicLong requestIdGen = new AtomicLong(0);

    @Override
    public RpcResponse sendRequest(RpcRequest request, String host, int port) {
        Channel channel = channelCache.computeIfAbsent(host + ":" + port, k -> createChannel(host, port));

        long requestId = requestIdGen.incrementAndGet();
        request.setRequestId(requestId);

        CompletableFuture<RpcResponse> future = new CompletableFuture<>();
        pending.put(requestId, future);

        channel.writeAndFlush(request);

        try {
            return future.get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            pending.remove(requestId);
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
                                .addLast(new RpcEncoder())
                                .addLast(new LengthFieldBasedFrameDecoder(
                                        Integer.MAX_VALUE, 0, 4, 0, 4))
                                .addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) {
                                        byte[] bytes = new byte[buf.readableBytes()];
                                        buf.readBytes(bytes);
                                        try (ObjectInputStream ois = new ObjectInputStream(
                                                new ByteArrayInputStream(bytes))) {
                                            RpcResponse response = (RpcResponse) ois.readObject();
                                            CompletableFuture<RpcResponse> f =
                                                    pending.remove(response.getRequestId());
                                            if (f != null) f.complete(response);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
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

    @ChannelHandler.Sharable
    private static class RpcEncoder extends MessageToByteEncoder<Object> {
        @Override
        protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                oos.writeObject(msg);
            }
            byte[] bytes = bos.toByteArray();
            out.writeInt(bytes.length);
            out.writeBytes(bytes);
        }
    }

    @Override
    public void close() {
        channelCache.values().forEach(Channel::close);
        group.shutdownGracefully();
    }
}
