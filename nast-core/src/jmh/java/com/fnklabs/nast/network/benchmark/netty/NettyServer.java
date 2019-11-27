package com.fnklabs.nast.network.benchmark.netty;

import com.google.common.net.HostAndPort;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.io.Closeable;
import java.io.IOException;

public class NettyServer implements Closeable {

    private final int acceptPool;
    private final int workerPool;
    private final EventLoopGroup parentGroup;
    private final EventLoopGroup childGroup;

    public NettyServer(HostAndPort address, ReplyHandler replyHandler, int acceptPool, int workerPool) throws InterruptedException {
        this.acceptPool = acceptPool;
        this.workerPool = workerPool;

        // (2)
        ServerBootstrap b = new ServerBootstrap();
        parentGroup = bossGroup();
        childGroup = workerGroup();
        b.group(parentGroup, childGroup)
         .channel(NioServerSocketChannel.class) // (3)
         .childHandler(new ChannelInitializer<SocketChannel>() {
             @Override
             protected void initChannel(SocketChannel ch) throws Exception {
                 ChannelPipeline pipeline = ch.pipeline();

                 pipeline.addLast(FixedLengthFrameDecoder.class.getName(), new FixedLengthFrameDecoder(12))
                         .addLast(replyHandler);
             }
             // (4)
         })
         .option(ChannelOption.SO_BACKLOG, 128)          // (5)
         .childOption(ChannelOption.SO_KEEPALIVE, true) // (6)
         .childOption(ChannelOption.TCP_NODELAY, true) // (6)
         .childOption(ChannelOption.SO_RCVBUF, 1024)
         .childOption(ChannelOption.SO_SNDBUF, 1024); // (6)

        // Bind and start to accept incoming connections.
        ChannelFuture f = b.bind(address.getHost(), address.getPort()).sync(); // (7)


    }

    private EventLoopGroup bossGroup() {
        return new NioEventLoopGroup(acceptPool); // (1)
    }

    private EventLoopGroup workerGroup() {
        return new NioEventLoopGroup(workerPool);
    }


    @Override
    public void close() throws IOException {
        parentGroup.shutdownGracefully();
        childGroup.shutdownGracefully();
    }
}
