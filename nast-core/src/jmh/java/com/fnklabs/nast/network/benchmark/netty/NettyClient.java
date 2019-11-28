package com.fnklabs.nast.network.benchmark.netty;

import com.google.common.net.HostAndPort;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class NettyClient implements Closeable {
    private final EventLoopGroup group;
    private final Channel channel;

    public ClientHandler clientHandler = new ClientHandler();
    ;

    public NettyClient(HostAndPort hostAndPort) throws InterruptedException {
        // Configure SSL.git


        // Configure the client.
        group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioSocketChannel.class)
             .option(ChannelOption.TCP_NODELAY, true)
             .handler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ChannelPipeline p = ch.pipeline();

                     //p.addLast(new LoggingHandler(LogLevel.INFO));

                     p.addLast(FixedLengthFrameDecoder.class.getName(), new FixedLengthFrameDecoder(12))
                      .addLast(clientHandler);
                 }
             });

            // Start the client.
            ChannelFuture f = b.connect(hostAndPort.getHost(), hostAndPort.getPort()).sync();

            channel = f.channel();

            // Wait until the connection is closed.
            f.channel().closeFuture();
        } finally {
            // Shut down the event loop to terminate all threads.

        }
    }

    public CompletableFuture<Integer> send(int id, ByteBuffer byteBuffer) {

        CompletableFuture<Integer> response = new CompletableFuture<>();

        ClientHandler.REPLY_FUTURES.put(id, response);

        ByteBuf msg = Unpooled.copiedBuffer(byteBuffer);

        channel.writeAndFlush(msg);

        return response;
    }

    public ChannelFuture write(int id, ByteBuffer byteBuffer) {

        CompletableFuture<Integer> response = new CompletableFuture<>();

        ClientHandler.REPLY_FUTURES.put(id, response);

        ByteBuf msg = Unpooled.copiedBuffer(byteBuffer);

       return channel.writeAndFlush(msg);
    }

    @Override
    public void close() throws IOException {
        group.shutdownGracefully();
    }
}
