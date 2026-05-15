package com.example.rpc;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import io.netty.handler.timeout.IdleStateHandler;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class NettyRpcServer {

    private final ServiceProvider serviceProvider;
    private final Serializer serializer;
    private final List<RpcFilter> filters;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public NettyRpcServer(ServiceProvider serviceProvider) {
        this(serviceProvider, new ObjectSerializer(), List.of());
    }

    public NettyRpcServer(ServiceProvider serviceProvider, Serializer serializer) {
        this(serviceProvider, serializer, List.of());
    }

    public NettyRpcServer(ServiceProvider serviceProvider, Serializer serializer, List<RpcFilter> filters) {
        this.serviceProvider = serviceProvider;
        this.serializer = serializer;
        this.filters = filters;
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
                                .addLast(new IdleStateHandler(15, 0, 0, TimeUnit.SECONDS))
                                .addLast(new RpcHeartbeatHandler(true))
                                .addLast(new RpcServerHandler(serviceProvider, filters));
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
        private final List<RpcFilter> filters;

        RpcServerHandler(ServiceProvider serviceProvider, List<RpcFilter> filters) {
            this.serviceProvider = serviceProvider;
            this.filters = filters;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RpcRequest request) {
            System.out.println("Received: " + request);

            RpcResponse response = new RpcResponse();
            response.setRequestId(request.getRequestId());

            try {
                for (RpcFilter filter : filters) {
                    if (!filter.before(request, response)) {
                        ctx.writeAndFlush(response);
                        return;
                    }
                }

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

            for (RpcFilter filter : filters) {
                filter.after(request, response);
            }

            ctx.writeAndFlush(response);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            System.err.println("server handler error: " + cause.getMessage());
            ctx.close();
        }
    }
}
