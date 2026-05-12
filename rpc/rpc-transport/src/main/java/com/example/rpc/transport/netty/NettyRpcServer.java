package com.example.rpc.transport.netty;

import com.example.rpc.core.serialization.Serializer;
import com.example.rpc.transport.RpcServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于 Netty 的 RPC 服务端
 */
public class NettyRpcServer implements RpcServer {

    private static final Logger log = LoggerFactory.getLogger(NettyRpcServer.class);

    private final Serializer serializer;
    private final ChannelHandler requestHandler;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public NettyRpcServer(Serializer serializer, ChannelHandler requestHandler) {
        this.serializer = serializer;
        this.requestHandler = requestHandler;
    }

    @Override
    public void start(int port) {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline()
                                    .addLast(new RpcMessageEncoder(serializer))
                                    .addLast(new RpcMessageDecoder())
                                    .addLast(requestHandler);
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.TCP_NODELAY, true);

            ChannelFuture future = bootstrap.bind(port).syncUninterruptibly();
            log.info("NettyRpcServer started on port {}", port);
        } catch (Exception e) {
            log.error("NettyRpcServer start failed", e);
            shutdown();
            throw new RuntimeException("start server failed", e);
        }
    }

    @Override
    public void shutdown() {
        if (workerGroup != null) workerGroup.shutdownGracefully();
        if (bossGroup != null) bossGroup.shutdownGracefully();
        log.info("NettyRpcServer shut down");
    }
}
