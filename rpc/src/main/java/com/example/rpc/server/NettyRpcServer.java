package com.example.rpc.server;
import com.example.rpc.filter.RpcFilter;
import com.example.rpc.protocol.MyDecode;
import com.example.rpc.protocol.MyEncode;
import com.example.rpc.protocol.ObjectSerializer;
import com.example.rpc.protocol.RpcHeartbeatHandler;
import com.example.rpc.protocol.RpcRequest;
import com.example.rpc.protocol.RpcResponse;
import com.example.rpc.protocol.Serializer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class NettyRpcServer {

    private static final Logger log = LoggerFactory.getLogger(NettyRpcServer.class);

    private static final int CORE_POOL = 10;
    private static final int MAX_POOL = 50;
    private static final int QUEUE_SIZE = 512;
    private static final int MAX_GLOBAL_CONCURRENCY = 200;
    private static final long PER_CONN_INTERVAL_NS = 1_000_000L; // 1000 rps per connection

    private static final AttributeKey<AtomicLong> CHANNEL_LIMITER = AttributeKey.valueOf("channelLimiter");

    private final ServiceProvider serviceProvider;
    private final Serializer serializer;
    private final List<RpcFilter> filters;
    private final ThreadPoolExecutor invokeExecutor;
    private final Semaphore globalLimiter;
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
        this.invokeExecutor = new ThreadPoolExecutor(CORE_POOL, MAX_POOL, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(QUEUE_SIZE),
                new ThreadPoolExecutor.AbortPolicy());
        this.globalLimiter = new Semaphore(MAX_GLOBAL_CONCURRENCY);
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
                        ch.attr(CHANNEL_LIMITER).set(new AtomicLong(0));
                        ch.pipeline()
                                .addLast(new MyEncode(serializer))
                                .addLast(new MyDecode())
                                .addLast(new IdleStateHandler(15, 0, 0, TimeUnit.SECONDS))
                                .addLast(new RpcHeartbeatHandler(true))
                                .addLast(new RpcServerHandler());
                    }
                });

        bootstrap.bind(port).syncUninterruptibly();
        log.info("NettyRpcServer started on port {}", port);
    }

    public void stop() {
        invokeExecutor.shutdown();
        if (workerGroup != null) workerGroup.shutdownGracefully();
        if (bossGroup != null) bossGroup.shutdownGracefully();
        log.info("NettyRpcServer stopped");
    }

    private class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RpcRequest request) {
            log.debug("Received: {}", request);

            // 第一层：全局并发限流
            if (!globalLimiter.tryAcquire()) {
                RpcResponse response = new RpcResponse();
                response.setRequestId(request.getRequestId());
                response.setCode(503);
                response.setMessage("server overloaded, try again later");
                ctx.writeAndFlush(response);
                log.warn("global limiter rejected request: {}", request.getRequestId());
                return;
            }

            // 第二层：连接级速率限流（CAS 无锁）
            AtomicLong channelLimiter = ctx.channel().attr(CHANNEL_LIMITER).get();
            if (channelLimiter != null) {
                long now = System.nanoTime();
                while (true) {
                    long pre = channelLimiter.get();
                    if (pre > now) {
                        globalLimiter.release();
                        RpcResponse response = new RpcResponse();
                        response.setRequestId(request.getRequestId());
                        response.setCode(429);
                        response.setMessage("connection rate limited");
                        ctx.writeAndFlush(response);
                        log.warn("channel limiter rejected request: {}", request.getRequestId());
                        return;
                    }
                    if (channelLimiter.compareAndSet(pre, now + PER_CONN_INTERVAL_NS)) {
                        break;
                    }
                }
            }

            // 提交到业务线程池
            try {
                invokeExecutor.submit(() -> processRequest(ctx, request));
            } catch (RejectedExecutionException e) {
                globalLimiter.release();
                RpcResponse response = new RpcResponse();
                response.setRequestId(request.getRequestId());
                response.setCode(503);
                response.setMessage("server thread pool full, try again later");
                ctx.writeAndFlush(response);
                log.warn("thread pool rejected request: {}", request.getRequestId());
            }
        }

        private void processRequest(ChannelHandlerContext ctx, RpcRequest request) {
            RpcResponse response = new RpcResponse();
            response.setRequestId(request.getRequestId());

            try {
                for (RpcFilter filter : filters) {
                    if (!filter.before(request, response)) {
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
                log.error("invoke error: {}", request, cause);
            } finally {
                for (RpcFilter filter : filters) {
                    filter.after(request, response);
                }
                globalLimiter.release();
                ctx.writeAndFlush(response);
            }
        }
    }

    @Override
    public String toString() {
        return "NettyRpcServer{pool=" + invokeExecutor.getPoolSize() +
                ", active=" + invokeExecutor.getActiveCount() +
                ", queue=" + invokeExecutor.getQueue().size() +
                ", globalPermits=" + globalLimiter.availablePermits() + "}";
    }
}
