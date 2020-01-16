package com.fnklabs.nast.network.io;

import com.fnklabs.nast.commons.Executors;
import com.fnklabs.nast.network.io.frame.SizeLimitDataFrameMarshaller;
import com.google.common.net.HostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.format;

public class ServerChannel extends AbstractNetworkChannel {
    /**
     * stub for linking {@link ServerSocketChannel} with selector to identify server {@link ServerSocketChannel}
     */
    private static final long SERVER_CHANNEL_ID = -2;

    private static final Logger log = LoggerFactory.getLogger(ServerChannel.class);
    /**
     * Client id sequence
     */
    private final AtomicLong CHANNEL_ID_SEQUENCE = new AtomicLong(0);

    private final ServerSocketChannel serverSocketChannel;

    private final ThreadPoolExecutor opAcceptPoolExecutor;
    private final ChannelWorker opAcceptWorker;
    private final SocketOptionsConfigurer socketOptionsConfigurer;

    public ServerChannel(HostAndPort listenHostAndPort, ChannelHandler channelHandler, int connectorPoolSize) throws IOException {
        this(listenHostAndPort, channelHandler, SocketOptionsConfigurerBuilder.builder().build(), connectorPoolSize);
    }

    public ServerChannel(HostAndPort listenHostAndPort, ChannelHandler channelHandler, SocketOptionsConfigurer socketOptionsConfigurer, int connectorPoolSize) throws IOException {
        super(Executors.fixedPool(format("network.server.connector.io[%s]", listenHostAndPort), connectorPoolSize), channelHandler, new SizeLimitDataFrameMarshaller());
        this.socketOptionsConfigurer = socketOptionsConfigurer;
        opAcceptPoolExecutor = Executors.fixedPool(format("network.server.accept[%s]", listenHostAndPort), 1);

        log.info("create server channel on {}", listenHostAndPort);

        try {
            serverSocketChannel = ServerSocketChannel.open();

            ServerSocket socket = serverSocketChannel.socket();
            socket.bind(new InetSocketAddress(listenHostAndPort.getHost(), listenHostAndPort.getPort()));

            if (!socket.isBound()) {
                throw new NetworkException(String.format("can't bound on address %s", listenHostAndPort));
            }


            serverSocketChannel.configureBlocking(false);


            while (!serverSocketChannel.isOpen()) {

            }

            opAcceptWorker = new ChannelWorker(this::processOpAccept);

            opAcceptPoolExecutor.submit(opAcceptWorker);

            opAcceptWorker.attach(selector -> {
                getLogger().debug("register SelectionKey.OP_ACCEPT channel {} to selector {}", serverSocketChannel, selector);

                try {
                    selector.wakeup();

                    return register(selector, serverSocketChannel, SelectionKey.OP_ACCEPT, null);

                } catch (ClosedChannelException e) {
                    log.warn("can't register op keys", e);
                }

                return null;
            });

            while (!serverSocketChannel.isRegistered()) {

            }
        } catch (BindException e) {
            log.warn("can't bind {}", listenHostAndPort, e);

            throw e;
        }
    }

    @Override
    public void close() throws Exception {
        log.debug("closing server channel {}...", serverSocketChannel);

        opAcceptWorker.close();

        Executors.shutdown(opAcceptPoolExecutor);

        super.close();

        try {
            serverSocketChannel.close();
        } catch (IOException e) {
            log.warn("can't close selector", e);
        }

        log.debug("{} closed", serverSocketChannel);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected ChannelWorker createWorker() {
        return new ChannelWorker(selector -> processIo(selector));
    }

    @Override
    public void processOpWrite(SelectionKey key) {
        ChannelSession channelSession = (ChannelSession) key.attachment();

        processOpWrite(channelSession);
    }

    @Override
    public void processOpRead(SelectionKey key) {
        SocketChannel clientSocketChannel = (SocketChannel) key.channel();

        ChannelSession channelSession = (ChannelSession) key.attachment();

        processOpRead(key, clientSocketChannel, channelSession);
    }

    @Override
    public void processOpAccept(SelectionKey key) {
        try {
            long channelId = getNextChannelId();

            SocketChannel clientChannel = serverSocketChannel.accept();

            log.debug("new client connection {} {}", channelId, clientChannel);

            if (clientChannel != null) {
                log.info("new client connection: {}", clientChannel.getRemoteAddress());

                socketOptionsConfigurer.apply(clientChannel);

                clientChannel.configureBlocking(false);

                ChannelSession channelSession = createChannelSession(channelId, clientChannel); //  todo move queueSize to parameters

                SelectionKey selectionKey = registerSession(channelSession);
            }
        } catch (Exception e) {
            log.warn("can't process selector [`{}`], {}", key.attachment(), key, e);
        }
    }

    @Override
    protected void processOpConnect(SelectionKey selectionKey) {
        log.warn("process op connect for session {}", selectionKey.attachment());

        closeChannel(selectionKey, selectionKey.channel());
    }

    /**
     * Return next client id from sequence
     *
     * @return Next client id
     */
    private long getNextChannelId() {
        return CHANNEL_ID_SEQUENCE.incrementAndGet();
    }


}
