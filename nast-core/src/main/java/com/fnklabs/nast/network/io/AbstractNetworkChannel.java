package com.fnklabs.nast.network.io;

import com.fnklabs.metrics.Metrics;
import com.fnklabs.metrics.MetricsFactory;
import com.fnklabs.nast.commons.Executors;
import com.fnklabs.nast.network.io.frame.DataFrameMarshaller;
import com.fnklabs.nast.network.io.frame.FrameException;
import org.slf4j.Logger;

import java.io.IOException;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;


abstract class AbstractNetworkChannel implements AutoCloseable {
    protected static final Metrics METRICS = MetricsFactory.getMetrics();
    /**
     * Flag that determine whether client is running
     */
    protected final AtomicBoolean isRunning = new AtomicBoolean(true);
    /**
     * Connector pool executor
     */
    private final ThreadPoolExecutor executorService;
    private final List<ChannelWorker> workers = new ArrayList<>();
    private final ChannelHandler channelHandler;
    private final DataFrameMarshaller dataFrameMarshaller;

    AbstractNetworkChannel(ThreadPoolExecutor executorService, ChannelHandler channelHandler, DataFrameMarshaller dataFrameMarshaller) throws ConnectionException {
        this.executorService = executorService;
        this.channelHandler = channelHandler;
        this.dataFrameMarshaller = dataFrameMarshaller;

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

        isRunning.set(false);

        for (ChannelWorker worker : workers) {
            try {
                worker.close();
            } catch (IOException e) {
                getLogger().warn("can't close worker", e);
            }
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
     * @param channelId channel id
     *
     * @return Selection key
     */
    protected SelectionKey register(Selector selector, SelectableChannel channel, int operation, long channelId) throws ClosedChannelException {
        selector.wakeup();

        getLogger().debug("wakeup selector and register operations for channel {}", channelId);

        SelectionKey register = channel.register(selector, operation, channelId);

        getLogger().debug("channel {} was registered on selector {} {}", channel, selector, register);

        return register;
    }

    /**
     * Blocking method that select keys from selector.
     * If keys was selected then selected keys will be applied to {@code keyConsumer}
     *
     * @param selector    Selector from which will be selected keys
     * @param keyConsumer New {@link SelectionKey} supplier
     */
    protected void select(Selector selector, Consumer<SelectionKey> keyConsumer) {
        try {
            int select = selector.select();

            if (select != 0) {

                Set<SelectionKey> selectionKeys = selector.selectedKeys();

                List<SelectionKey> keys = new ArrayList<>(selectionKeys);

                Iterator<SelectionKey> keysIterator = keys.iterator();

                while (isRunning.get() && keysIterator.hasNext()) {
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
     * @param channelID     {@link ChannelSession#id} value
     * @param socketChannel {@link ChannelSession#socketChannel} value
     *
     * @return Channel session instance
     */
    ChannelSession createChannelSession(long channelID, SocketChannel socketChannel) {
        return new ChannelSession(channelID, socketChannel);
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
     * Gracefully shutdown selectable channel by canceling from {@link Selector}
     * and close {@link SelectableChannel}
     *
     * @param key               {@link SelectionKey} associated key with selector that could be canceled
     * @param selectableChannel SocketChannel that must be closed
     */
    void closeChannel(SelectionKey key, SelectableChannel selectableChannel) {
        key.cancel();

        try {
            selectableChannel.close();
        } catch (IOException e) {
            getLogger().warn("Can't close channel", e);
        }
    }

    /**
     * Register client session for {@link SelectionKey#OP_READ} {@link SelectionKey#OP_WRITE} {@link SelectionKey#OP_CONNECT} operations
     * in less loaded worker/selector
     *
     * @param channelSession ChannelSession that must be registered in {@link Selector}/{@link ChannelWorker}
     *
     * @return SelectionKey associated with {@link Selector}
     */
    SelectionKey registerSession(ChannelSession channelSession) {
        return getLessLoadedWorker().add(selector -> {
            getLogger().debug("register SelectionKey.OP_READ | SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT channel {} to selector {}", channelSession.getSocketChannel(), selector);

            try {
                return register(selector, channelSession.getSocketChannel(), SelectionKey.OP_READ | SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT, channelSession.getId());
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

        // data for write already in buffer
        // todo add opportunity to write several messages to buffer
        if (!channelSession.getPendingWriteOperations().isEmpty() && outBuffer.remaining() > 0) {
            getLogger().debug("write data from session.outBuffer (pending operations)");

            try {
                int writtenData = write(channelSession.getSocketChannel(), outBuffer);

                getLogger().debug("`{}` bytes was send from out buffer {} response was send to channel [{}] total send bytes: {}", writtenData, outBuffer, channelSession.getId(), writtenData);

                // remove pending operation future

                if (outBuffer.remaining() == 0) {

                    METRICS.getCounter(String.format("network.%s.io.frm.send", getClass().getSimpleName())).inc();

                    for (WriteFuture pendingWriteOperation : channelSession.getPendingWriteOperations()) {
                        pendingWriteOperation.complete(null);
                    }

                    channelSession.getPendingWriteOperations().clear();

                    outBuffer.compact();

                } else {
                    getLogger().debug("remaining data is available try to write later");
                    return;
                }
            } catch (HostNotAvailableException e) {
                getLogger().warn("host is not available. complete pending future with exception");

                for (WriteFuture pendingWriteOperation : channelSession.getPendingWriteOperations()) {
                    pendingWriteOperation.completeExceptionally(e);
                }

                channelSession.getPendingWriteOperations().clear();
            }

        }

        if (channelSession.getPendingWriteOperations().isEmpty()) {

            // try write to out buffer
            WriteFuture writeFuture = channelHandler.onWrite(channelSession);//channelSession.getOutgoingMsgQueue().peek();


            if (writeFuture != null) {
                getLogger().debug("write data to out buffer");

                ByteBuffer dataBuffer = writeFuture.getBuffer();

                try {
                    dataFrameMarshaller.encode(dataBuffer, outBuffer);

                    METRICS.getCounter(String.format("network.%s.io.frm.encode", getClass().getSimpleName())).inc();

                    channelSession.getPendingWriteOperations().add(writeFuture);

                    outBuffer.flip();
                } catch (FrameException e) {
                    METRICS.getCounter(String.format("network.%s.io.frm.encode.failure", getClass().getSimpleName())).inc();

                    writeFuture.completeExceptionally(e);

                } finally {

                }
            }
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

        try {
            int readBytes = read(clientSocketChannel, channelInBuffer);

            getLogger().debug("flip buffer {} after read {} bytes", channelInBuffer, readBytes);

            channelInBuffer.flip();

            getLogger().debug("buffer was flipped {}", channelInBuffer);

            if (readBytes > 0 || channelInBuffer.remaining() > 0) {

                getLogger().debug("data was read from channel {} {}", channelSession.getId(), readBytes);

                for (; ; ) {

                    getLogger().debug("get frame from buffer {}", channelInBuffer);

                    ByteBuffer frm = dataFrameMarshaller.decode(channelInBuffer);

                    getLogger().debug("get frame {} from buffer {}", frm, channelInBuffer);

                    if (frm == null) {
                        break;
                    }

                    METRICS.getCounter(String.format("network.%s.io.frm.decode", getClass().getSimpleName())).inc();

                    CompletableFuture<Void> handleFuture = channelHandler.onRead(channelSession, frm);

                    handleFuture.exceptionally(e -> {
                        METRICS.getCounter(String.format("network.%s.io.frm.handle.failure", getClass().getSimpleName())).inc();
                        getLogger().warn("can't handle frame for channel {}", channelSession, e);

                        return null;
                    });

                    handleFuture.thenAccept(r -> {
                        METRICS.getCounter(String.format("network.%s.io.frm.handle.success", getClass().getSimpleName())).inc();

                        getLogger().debug("handle future was completed");
                    });

                }

            }


        } catch (ChannelClosedException e) {
            closeChannel(key, clientSocketChannel);
        } catch (Exception e) {
            getLogger().warn("can't process selector [`{}`], {}", key.attachment(), key, e);
        } finally {
            if (channelInBuffer.position() != 0) {
                getLogger().debug("compacting buffer {} before op read", channelInBuffer);
                channelInBuffer.compact();

                getLogger().debug("buffer was compacted {} before op read", channelInBuffer);
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
    protected void processOp(SelectionKey selectionKey) throws CancelledKeyException {
        if (selectionKey.isValid() && selectionKey.isConnectable()) {
            this.processOpConnect(selectionKey);
        }

        if (selectionKey.isValid() && selectionKey.isAcceptable()) {
            this.processOpAccept(selectionKey);
        }

        if (selectionKey.isValid() && selectionKey.isWritable()) {
            this.processOpWrite(selectionKey);
        }

        if (selectionKey.isValid() && selectionKey.isReadable()) {
            this.processOpRead(selectionKey);
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

            socketChannel.socket().getOutputStream().flush();

            METRICS.getCounter(String.format("network.%s.io.write", getClass().getSimpleName())).inc(writtenData);

            getLogger().debug("write `{}` bytes to channel: {}", writtenData, socketChannel.getRemoteAddress());

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
            int receivedBytes = socketChannel.read(buffer);

            getLogger().debug("received `{}` bytes: from {}", receivedBytes, socketChannel.getRemoteAddress());

            METRICS.getCounter(String.format("network.%s.io.read", getClass().getSimpleName())).inc(receivedBytes);

            /* Check if socket was closed */
            if (receivedBytes == -1) {
                throw new ChannelClosedException(socketChannel);
            }

            return receivedBytes;
        } catch (IOException e) {
            // todo handle java.io.IOException: Connection reset by peer exception
            getLogger().error("Can't read from channel {}/{}", socketChannel, e);
        }

        return 0;
    }


}
