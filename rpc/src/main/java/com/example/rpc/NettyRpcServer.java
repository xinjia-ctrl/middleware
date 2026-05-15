package com.example.rpc;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.lang.reflect.Method;

public class NettyRpcServer {

    private final ServiceProvider serviceProvider;
    private final Serializer serializer;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public NettyRpcServer(ServiceProvider serviceProvider) {
        this(serviceProvider, new ObjectSerializer());
    }

    public NettyRpcServer(ServiceProvider serviceProvider, Serializer serializer) {
        this.serviceProvider = serviceProvider;
        this.serializer = serializer;
    }

    public void start(int port) {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new MyEncode(serializer))
                                .addLast(new MyDecode())
                                .addLast(new RpcServerHandler(serviceProvider));
                    }
                });

        bootstrap.bind(port).syncUninterruptibly();
        System.out.println("NettyRpcServer started on port " + port);
    }

    public void stop() {
        if (workerGroup != null) workerGroup.shutdownGracefully();
        if (bossGroup != null) bossGroup.shutdownGracefully();
        System.out.println("NettyRpcServer stopped");
    }

    private static class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

        private final ServiceProvider serviceProvider;

        RpcServerHandler(ServiceProvider serviceProvider) {
            this.serviceProvider = serviceProvider;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RpcRequest request) {
            System.out.println("Received: " + request);

            RpcResponse response = new RpcResponse();

            try {
                Object serviceImpl = serviceProvider.getService(request.getInterfaceName());
                if (serviceImpl == null) {
                    response.setCode(500);
                    response.setMessage("service not found: " + request.getInterfaceName());
                } else {
                    Method method = serviceImpl.getClass().getMethod(
                            request.getMethodName(), request.getParameterTypes());
                    Object result = method.invoke(serviceImpl, request.getParameters());
                    response.setCode(200);
                    response.setData(result);
                }
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                response.setCode(500);
                response.setMessage(cause.getMessage());
            }
            response.setRequestId(request.getRequestId());
            ctx.writeAndFlush(response);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            System.err.println("server handler error: " + cause.getMessage());
            ctx.close();
        }
    }
}
