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
import java.net.StandardSocketOptions;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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

    private final Map<Long, ChannelSession> sessions = new TreeMap<>();

    private final ServerSocketChannel serverSocketChannel;

    private int channelQueueSize;

    public ServerChannel(HostAndPort listenHostAndPort, ChannelHandler channelHandler, int connectorPoolSize) throws IOException {
        super(Executors.fixedPool(format("network.server.connector.io[%s]", listenHostAndPort), connectorPoolSize), channelHandler, new SizeLimitDataFrameMarshaller());

        this.channelQueueSize = 1;

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

            getLessLoadedWorker().add(selector -> {
                getLogger().debug("register SelectionKey.OP_ACCEPT channel {} to selector {}", serverSocketChannel, selector);

                try {
                    selector.wakeup();

                    SelectionKey selectionKey = register(selector, serverSocketChannel, SelectionKey.OP_ACCEPT, SERVER_CHANNEL_ID);


                    Set<SelectionKey> keys = selector.keys();

                    log.debug("current keys {}", keys);

                    return selectionKey;


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
        return new ChannelWorker(selector -> {
            if (isRunning.get()) {
                select(selector, this::processOp);
            }

            return isRunning.get();
        });
    }

    @Override
    protected void closeChannel(SelectionKey key, SelectableChannel selectableChannel) {

        long channelId = (long) key.attachment();

        log.debug("close channel {}/{}", channelId, selectableChannel);

        ChannelSession channelSession = sessions.remove(channelId);

        if (channelSession != null) {
            channelSession.close();

        }

        super.closeChannel(key, selectableChannel);

        // todo decrement worker weight
    }

    @Override
    public void processOpWrite(SelectionKey key) {
        ChannelSession channelSession = sessions.get(key.attachment());

        try {
            processOpWrite(channelSession);
        } catch (Exception e) {
            log.warn("can't write data for channel {}/{}", key.attachment(), channelSession, e);
        }

    }

    @Override
    public void processOpRead(SelectionKey key) {
        SocketChannel clientSocketChannel = (SocketChannel) key.channel();

        long channelId = (long) key.attachment();

        log.debug("new read event from client: {}", channelId);

        ChannelSession channelSession = sessions.get(channelId);

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

                clientChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
                clientChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
                clientChannel.setOption(StandardSocketOptions.SO_SNDBUF, 1024);
                clientChannel.setOption(StandardSocketOptions.SO_RCVBUF, 1024);
                clientChannel.configureBlocking(false);


                ChannelSession channelSession = createChannelSession(channelId, clientChannel); //  todo move queueSize to parameters

                sessions.put(channelId, channelSession);

                SelectionKey selectionKey = registerSession(channelSession);

                channelSession.onClose(id -> closeChannel(selectionKey, clientChannel));
            }
        } catch (Exception e) {
            log.warn("can't process selector [`{}`], {}", key.attachment(), key, e);
        }
    }

    @Override
    protected void processOpConnect(SelectionKey selectionKey) {
        long channelId = (long) selectionKey.attachment();

        ChannelSession channelSession = sessions.remove(channelId);

        log.warn("process op connect for channel {}/{}", channelId, channelSession);

        if (channelSession != null) {
            channelSession.close();
        }
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
