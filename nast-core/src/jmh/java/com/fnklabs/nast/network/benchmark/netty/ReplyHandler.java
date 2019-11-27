package com.fnklabs.nast.network.benchmark.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.nio.ByteBuffer;

@ChannelHandler.Sharable
public class ReplyHandler extends ChannelInboundHandlerAdapter {


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuf = (ByteBuf) msg;

        int requestId = byteBuf.readInt();
        int valueId = byteBuf.readInt();

        ByteBuffer buffer = ByteBuffer.allocate(12);
        buffer.putInt(requestId);
        buffer.putInt(valueId);
        buffer.putInt(valueId);
        buffer.rewind();

        ctx.write(Unpooled.copiedBuffer(buffer));
        ctx.flush();

        ReferenceCountUtil.release(msg);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}
