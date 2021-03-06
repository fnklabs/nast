package com.fnklabs.nast.network.io;

import com.fnklabs.nast.commons.Executors;
import com.fnklabs.nast.network.io.frame.SizeLimitDataFrameMarshaller;
import com.google.common.net.HostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Calendar;
import java.util.concurrent.CompletableFuture;

public class ClientChannel extends AbstractNetworkChannel {

    private static final Logger log = LoggerFactory.getLogger(ClientChannel.class);

    /**
     * stub for linking client {@link SocketChannel} with selector to identify client {@link SocketChannel}
     */
    private static final long CLIENT_CHANNEL_ID = -1;

    /**
     * Remote server address
     */
    private final HostAndPort remoteAddress;

    private final ClientChannelHandler channelHandler;

    /**
     * Client channel
     */
    private final SocketChannel channel;

    /**
     * Client channel session (stub) for compatible with {@link AbstractNetworkChannel} functionality
     */
    private final ChannelSession channelSession;

    private final SocketOptionsConfigurer socketOptionsConfigurer;

    public ClientChannel(HostAndPort remoteAddress, ClientChannelHandler channelHandler) throws ConnectionException {
        this(remoteAddress, channelHandler, SocketOptionsConfigurerBuilder.builder().build(), Integer.parseInt(System.getProperty("network.client.in-buf", "65536")), Integer.parseInt(System.getProperty("network.client.out-buf", "65536")));
    }

    public ClientChannel(HostAndPort remoteAddress, ClientChannelHandler channelHandler, SocketOptionsConfigurer socketOptionsConfigurer,
                         int clientChannelInBufferSize, int clientChannelOutBufferSize) throws ConnectionException {
        super(Executors.fixedPool(String.format("network.client.connector.io[%s]", remoteAddress), 1), channelHandler, new SizeLimitDataFrameMarshaller(), clientChannelInBufferSize, clientChannelOutBufferSize);

        this.remoteAddress = remoteAddress;
        this.channelHandler = channelHandler;
        this.socketOptionsConfigurer = socketOptionsConfigurer;

        log.warn("building client: {}", remoteAddress);

        try {
            channel = SocketChannel.open();

            socketOptionsConfigurer.apply(channel);

            channel.configureBlocking(false);

            channel.connect(new InetSocketAddress(remoteAddress.getHost(), remoteAddress.getPort()));

            while (!channel.isConnectionPending()) {
                log.debug("connection is pending");
            }

            while (!channel.finishConnect()) {
                // await connect
            }

            channelSession = createChannelSession(CLIENT_CHANNEL_ID, channel);

            // Join thread pool for processing selectorOpRead|selectorOpWrite events (must be called only once)

            SelectionKey selectionKey = registerSession(channelSession);

            while (!channelSession.getSocketChannel().isRegistered()) {
                // await registering in worker
            }

            log.info("client was successfully connected to {}", remoteAddress);

        } catch (IOException e) {
            throw new ConnectionException("could not connect to host", remoteAddress, e);
        }
    }


    @Override
    public void close() throws Exception {
        super.close();

        channel.close();

        log.info("Close connector: {}", remoteAddress);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected ChannelWorker createWorker() {
        return new ChannelWorker(this::processIo);
    }

    @Override
    protected void processOpWrite(SelectionKey key) {
        processOpWrite(channelSession);
    }

    @Override
    protected void processOpRead(SelectionKey key) {
        SocketChannel clientSocketChannel = (SocketChannel) key.channel();

        processOpRead(key, clientSocketChannel, channelSession);
    }

    /**
     * Send message to remote server
     *
     * @param data Data that must be sent
     *
     * @return Future for write to {@link SocketChannel}
     */
    public CompletableFuture<Void> send(ByteBuffer data) {
        return channelHandler.send(data);
    }

    @Override
    void closeChannel(SelectionKey key, SelectableChannel selectableChannel) {
        try {
            super.closeChannel(key, selectableChannel);
        } catch (SessionClosed e) {
            throw new StopWorker();
        }
    }
}
