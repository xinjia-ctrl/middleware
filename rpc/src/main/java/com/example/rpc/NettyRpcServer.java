package com.example.rpc;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NettyRpcServer {

    private final Map<String, Object> serviceMap = new ConcurrentHashMap<>();
    private final Serializer serializer;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public NettyRpcServer() {
        this(new ObjectSerializer());
    }

    public NettyRpcServer(Serializer serializer) {
        this.serializer = serializer;
    }

    public void register(Class<?> interfaceClass, Object serviceImpl) {
        serviceMap.put(interfaceClass.getName(), serviceImpl);
        System.out.println("register service: " + interfaceClass.getName());
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
                                .addLast(new RpcServerHandler());
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

    private class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RpcRequest request) {
            System.out.println("Received: " + request);

            RpcResponse response;
            try {
                Object serviceImpl = serviceMap.get(request.getInterfaceName());
                if (serviceImpl == null) {
                    response = new RpcResponse(500, null,
                            "service not found: " + request.getInterfaceName());
                } else {
                    Method method = serviceImpl.getClass().getMethod(
                            request.getMethodName(), request.getParameterTypes());
                    Object result = method.invoke(serviceImpl, request.getParameters());
                    response = new RpcResponse(200, result, null);
                }
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                response = new RpcResponse(500, null, cause.getMessage());
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
