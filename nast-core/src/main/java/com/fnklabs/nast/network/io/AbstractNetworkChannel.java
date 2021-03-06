package com.fnklabs.nast.network.io;

import com.fnklabs.nast.commons.Executors;
import com.fnklabs.nast.network.io.frame.DataFrameMarshaller;
import com.fnklabs.nast.network.io.frame.FrameException;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.SocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;


/**
 * Abstract network channel that provide base methods for
 * <ul>
 *     <li>Register SocketChannel at Selector</li>
 *     <li>Select new events from Selector</li>
 *     <li>processOpRead events</li>
 *     <li>processOpWrite events</li>
 *     <li>processOpAccept events</li>
 *     <li>processOpConnect events</li>
 *     <li>Close channel</li>
 * </ul>
 */
abstract class AbstractNetworkChannel implements AutoCloseable {
    /**
     * Connector pool executor
     */
    private final ThreadPoolExecutor executorService;
    private final List<ChannelWorker> workers = new ArrayList<>();
    private final ChannelHandler channelHandler;
    private final DataFrameMarshaller dataFrameMarshaller;

    private final int channelInBufferSize;
    private final int channelOutBufferSize;

    AbstractNetworkChannel(ThreadPoolExecutor executorService, ChannelHandler channelHandler, DataFrameMarshaller dataFrameMarshaller, int channelInBufferSize, int channelOutBufferSize) throws ConnectionException {
        this.executorService = executorService;
        this.channelHandler = channelHandler;
        this.dataFrameMarshaller = dataFrameMarshaller;
        this.channelInBufferSize = channelInBufferSize;
        this.channelOutBufferSize = channelOutBufferSize;

        for (int i = 0; i < executorService.getCorePoolSize(); i++) {
            getLogger().debug("create selector worker {}", i);

            ChannelWorker worker = createWorker();

            workers.add(worker);

            this.executorService.submit(worker);
        }
    }

    /**
     * Gracefully shutdown IO workers {@link ChannelWorker} and {@link ChannelHandler}
     */
    @Override
    public void close() throws Exception {
        getLogger().debug("closing channel...");

        for (ChannelWorker worker : workers) {
            worker.close();
        }

        getLogger().debug("shutdown channel executors ...");

        Executors.shutdown(executorService);

        try {
            channelHandler.close();
        } catch (Exception e) {
            getLogger().warn("can't close channel handler", e);
        }

        getLogger().debug("channel was closed");

    }

    /**
     * Register provide channel in selector with operation
     *
     * @param selector  Selector that could be using for registering channel
     * @param channel   SocketChannel for registering
     * @param operation Socket operation
     * @param session   ChannelSession instance
     *
     * @return Selection key
     *
     * @throws ClosedChannelException on channel closed
     */
    protected SelectionKey register(Selector selector, SelectableChannel channel, int operation, ChannelSession session) throws ClosedChannelException {
        selector.wakeup();

        getLogger().debug("wakeup selector and register operations for channel {}", session);

        SelectionKey register = channel.register(selector, operation, session);

        getLogger().debug("channel {} was registered on selector {} {}", channel, selector, register);

        return register;
    }

    /**
     * Blocking method that select keys from selector. If keys was selected then selected keys will be applied to {@code keyConsumer}
     *
     * @param selector    Selector from which will be selected keys
     * @param keyConsumer New {@link SelectionKey} supplier
     */
    protected void select(Selector selector, Consumer<SelectionKey> keyConsumer) {
        try {
            int select = selector.select();

            if (select != 0) {

                Set<SelectionKey> selectionKeys = selector.selectedKeys();

                Iterator<SelectionKey> keysIterator = selectionKeys.iterator();

                while (keysIterator.hasNext()) {
                    try {
                        SelectionKey key = keysIterator.next();

                        selectionKeys.remove(key);

                        keyConsumer.accept(key);

                    } catch (ConcurrentModificationException e) {
                        break;
                    }

                }
            }
        } catch (ClosedSelectorException e) {
            throw e;
        } catch (Exception e) {
            getLogger().error("can't select keys", e);
        }
    }

    /**
     * Create channel session
     *
     * @param channelID     {@link ChannelSession#getId()} value
     * @param socketChannel {@link ChannelSession#getSocketChannel()} value
     *
     * @return Channel session instance
     */
    ChannelSession createChannelSession(long channelID, SocketChannel socketChannel) {
        return new ChannelSession(channelID, socketChannel, channelInBufferSize, channelOutBufferSize);
    }

    /**
     * Get logger instance
     *
     * @return Logger instance
     */
    abstract protected Logger getLogger();

    /**
     * Create worker instance
     *
     * @return {@link ChannelWorker} instance
     */
    abstract protected ChannelWorker createWorker();

    /**
     * Gracefully shutdown selectable channel by canceling from {@link Selector} and close {@link SelectableChannel}
     *
     * @param key               {@link SelectionKey} associated key with selector that could be canceled
     * @param selectableChannel SocketChannel that must be closed
     */
    void closeChannel(SelectionKey key, SelectableChannel selectableChannel) {
        ChannelSession session = (ChannelSession) key.attachment();
        getLogger().debug("close session {} channel {}", session, selectableChannel);

        key.cancel();

        try {
            selectableChannel.close();
        } catch (IOException e) {
            getLogger().warn("Can't close channel", e);
        }

        session.close();

        key.selector().wakeup();

        channelHandler.onDisconnect(session);

        throw new SessionClosed();
    }

    /**
     * Register client session for {@link SelectionKey#OP_READ} {@link SelectionKey#OP_WRITE} {@link SelectionKey#OP_CONNECT} operations in less loaded
     * worker/selector
     *
     * @param channelSession ChannelSession that must be registered in {@link Selector}/{@link ChannelWorker}
     *
     * @return SelectionKey associated with {@link Selector}
     */
    SelectionKey registerSession(ChannelSession channelSession) {
        return getLessLoadedWorker().attach(selector -> {

            try {
                return register(selector, channelSession.getSocketChannel(), SelectionKey.OP_READ | SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT, channelSession);
            } catch (ClosedChannelException e) {
                getLogger().warn("can't register channel", e);

                throw new NetworkException(e);
            }

        });
    }

    /**
     * Get less loading worker
     *
     * @return {@link ChannelWorker} instance
     */
    protected ChannelWorker getLessLoadedWorker() {
        ChannelWorker lessLoadedWorker = workers.get(0);

        for (ChannelWorker worker : workers) {
            if (worker.getConnections() < lessLoadedWorker.getConnections()) {
                lessLoadedWorker = worker;
            }
        }

        return lessLoadedWorker;
    }

    /**
     * Process {@link SelectionKey#OP_WRITE}
     *
     * @param key SelectionKey that available for write
     */
    protected abstract void processOpWrite(SelectionKey key);

    /**
     * Process write event
     *
     * @param channelSession ChannelSession that ready for write
     */
    protected void processOpWrite(ChannelSession channelSession) {
        ByteBuffer outBuffer = channelSession.getOutBuffer();

        // try write to out buffer
        WriteFuture writeFuture = channelHandler.onWrite(channelSession);


        if (writeFuture != null) {
            ByteBuffer dataBuffer = writeFuture.getBuffer();

            try {
                outBuffer.compact();

                dataFrameMarshaller.encode(dataBuffer, outBuffer);

                channelSession.getPendingWriteOperations()
                              .add(writeFuture);

                outBuffer.flip();
            } catch (FrameException e) {
                getLogger().warn("can't encode frame", e);

                writeFuture.completeExceptionally(e);
            }

            writeFuture.exceptionally(e -> {
                channelHandler.onException(e);

                return null;
            });
        }
        // data for write already in buffer
        // todo add opportunity to write several messages to buffer

        try {

            if (!channelSession.getPendingWriteOperations().isEmpty()) {
                int writtenData = write(channelSession.getSocketChannel(), outBuffer);

                getLogger().debug("data ({} bytes) was send to {}", writtenData, channelSession);
                // remove pending operation future

                if (writtenData > 0 && outBuffer.remaining() == 0) {

                    for (WriteFuture pendingWriteOperation : channelSession.getPendingWriteOperations()) {
                        pendingWriteOperation.complete(null);
                    }

                    channelSession.getPendingWriteOperations().clear();
                }
            }
        } catch (HostNotAvailableException e) {
            getLogger().warn("host is not available. complete pending future with exception");

            for (WriteFuture pendingWriteOperation : channelSession.getPendingWriteOperations()) {
                pendingWriteOperation.completeExceptionally(e);
            }

            channelSession.getPendingWriteOperations().clear();
        }

    }

    /**
     * Process {@link SelectionKey#OP_READ}
     *
     * @param key SelectionKey that available for read
     */
    protected abstract void processOpRead(SelectionKey key);

    /**
     * Process read event
     *
     * @param key                 {@link SelectionKey} that for read event
     * @param clientSocketChannel SocketChannel for read
     * @param channelSession      ChannelSession that related to {@link SelectionKey}
     */
    protected void processOpRead(SelectionKey key, SocketChannel clientSocketChannel, ChannelSession channelSession) {
        ByteBuffer channelInBuffer = channelSession.getInBuffer();

        getLogger().debug("process opRead for {} key operations isValid {} isReadable {} isWritable {} isConnectable {}", channelSession,
                          key.isValid(), key.isReadable(), key.isWritable(), key.isConnectable()
        );

        int readBytes = read(clientSocketChannel, channelInBuffer);

        /* Check if socket was closed */
        if (readBytes == -1) {
            getLogger().warn("channel was closed: {} session: {}", clientSocketChannel, channelSession);
            throw new ChannelClosedException(clientSocketChannel);
        }

        getLogger().debug("received {} bytes from {}", readBytes, channelSession);

        channelInBuffer.flip();

        if (readBytes > 0) {

            for (; ; ) {

                ByteBuffer frm = dataFrameMarshaller.decode(channelInBuffer);

                if (frm == null) {
                    if (channelInBuffer.remaining() > 0) {
                        channelInBuffer.compact();
                    } else {
                        channelInBuffer.clear();
                    }

                    break;
                }

                channelHandler.onRead(channelSession, frm);
            }
        }
    }

    /**
     * Accept new connection and process it
     *
     * @param key New socket channel
     */
    protected void processOpAccept(SelectionKey key) { throw new IllegalStateException("no-op"); }

    protected void processOpConnect(SelectionKey selectionKey) {throw new IllegalStateException("no-op");}

    /**
     * Process {@link SelectionKey} key
     *
     * @param selectionKey Selection key that ready for new events
     *
     * @throws CancelledKeyException on key was canceled
     */
    protected void processIo(SelectionKey selectionKey) throws CancelledKeyException {
        try {
            if (!selectionKey.isValid()) {
                return;
            }
            if (selectionKey.isConnectable()) {
                this.processOpConnect(selectionKey);
            }

            if (selectionKey.isReadable()) {
                this.processOpRead(selectionKey);
            }

            if (selectionKey.isWritable()) {
                this.processOpWrite(selectionKey);
            }
        } catch (ChannelClosedException e) {
            closeChannel(selectionKey, selectionKey.channel());
        } catch (HostNotAvailableException e) {
            closeChannel(selectionKey, selectionKey.channel());
        }
    }

    /**
     * Write data from {@link ByteBuffer} to {@link SocketChannel}
     *
     * @param socketChannel {@link SocketChannel} for write data from {@link ByteBuffer}
     * @param data          {@link ByteBuffer} that contains data
     *
     * @return written bytes
     *
     * @throws HostNotAvailableException if connection was closed and data wasn't send
     */
    private int write(SocketChannel socketChannel, ByteBuffer data) throws HostNotAvailableException {
        try {
            int writtenData = socketChannel.write(data);

            return writtenData;
        } catch (Exception e) {
            getLogger().warn("can't write data to channel {}", socketChannel, e);

            throw new HostNotAvailableException();
        }
    }

    /**
     * Read data from {@link SocketChannel} to {@link ByteBuffer}
     *
     * @param socketChannel {@link SocketChannel} to read data
     * @param buffer        Buffer that will be used for reading data
     *
     * @return reading bytes
     *
     * @throws ChannelClosedException if connection was closed and data wasn't send
     */
    private int read(SocketChannel socketChannel, ByteBuffer buffer) throws ChannelClosedException {
        try {
            return socketChannel.read(buffer);
        } catch (IOException e) {
            // todo handle java.io.IOException: Connection reset by peer exception
            getLogger().error("Can't read from channel {}/{}", socketChannel, e);

            throw new ChannelClosedException(socketChannel);
        }
    }
}
