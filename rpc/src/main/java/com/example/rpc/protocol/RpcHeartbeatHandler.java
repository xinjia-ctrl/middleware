package com.example.rpc.protocol;
import com.example.rpc.config.RpcConstants;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcHeartbeatHandler extends ChannelDuplexHandler {

    private static final Logger log = LoggerFactory.getLogger(RpcHeartbeatHandler.class);
    private final boolean serverSide;

    public RpcHeartbeatHandler(boolean serverSide) {
        this.serverSide = serverSide;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            if (serverSide) {
                log.info("client idle timeout, close connection: {}", ctx.channel().remoteAddress());
                ctx.close();
            } else {
                log.debug("send heartbeat to {}", ctx.channel().remoteAddress());
                ctx.writeAndFlush(RpcHeartbeat.INSTANCE);
            }
            return;
        }
        ctx.fireUserEventTriggered(evt);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof RpcHeartbeat) {
            if (serverSide) {
                log.debug("receive heartbeat from {}, echo back", ctx.channel().remoteAddress());
                ctx.writeAndFlush(msg);
            }
            return;
        }
        ctx.fireChannelRead(msg);
    }
}
