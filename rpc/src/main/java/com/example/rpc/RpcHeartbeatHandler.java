package com.example.rpc;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;

public class RpcHeartbeatHandler extends ChannelDuplexHandler {

    private final boolean serverSide;

    public RpcHeartbeatHandler(boolean serverSide) {
        this.serverSide = serverSide;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            if (serverSide) {
                ctx.close();
            } else {
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
                ctx.writeAndFlush(msg);
            }
            return;
        }
        ctx.fireChannelRead(msg);
    }
}
