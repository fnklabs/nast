package com.fnklabs.nast.network.benchmark.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@ChannelHandler.Sharable
public class ClientHandler extends ChannelInboundHandlerAdapter {
    public static final Map<Integer, CompletableFuture<Integer>> REPLY_FUTURES = new ConcurrentHashMap<>();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
//        ctx.writeAndFlush(Unpooled.copiedBuffer("123\n".getBytes()));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;

        int requestId = buf.readInt();
        int value = buf.readInt();

        CompletableFuture<Integer> response = REPLY_FUTURES.remove(requestId);
        response.complete(value);

        ReferenceCountUtil.release(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}
