package com.fnklabs.nast.network.io;

import com.google.common.base.MoreObjects;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Current implementation is not thread safe and thread safe must is guarantied by
 * worker (only on thread could read/write data from channel)
 */
@NotThreadSafe
class ChannelSession implements Session, Closeable {
    private final long id;

    /**
     * Incoming messages buffer
     */
    private final ByteBuffer inBuffer;

    private final SocketChannel socketChannel;

    private final List<Consumer<Long>> listeners = new CopyOnWriteArrayList<>();

    private final ByteBuffer outBuffer;

    private final List<WriteFuture> pendingWriteOperations = new ArrayList<>();

    ChannelSession(long id, SocketChannel socketChannel) {
        this.id = id;
        this.socketChannel = socketChannel;

        inBuffer = ByteBuffer.allocateDirect(64 * 1024); // todo move buffer size to parameters
        outBuffer = ByteBuffer.allocateDirect(64 * 1024); // todo move buffer size to parameters
    }

    public List<WriteFuture> getPendingWriteOperations() {
        return pendingWriteOperations;
    }

    public ByteBuffer getOutBuffer() {
        return outBuffer;
    }

    @Override
    public long getId() {
        return id;
    }

    public ByteBuffer getInBuffer() {
        return inBuffer;
    }

    @Override
    public void close() {
        for (Consumer<Long> listener : listeners) {
            listener.accept(id);
        }
    }


    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("id", id)
                          .add("socketChannel", socketChannel)
                          .toString();
    }

    SocketChannel getSocketChannel() {
        return socketChannel;
    }

    /**
     * Add on close listener
     *
     * @param onCloseListener
     */
    void onClose(Consumer<Long> onCloseListener) {
        listeners.add(onCloseListener);
    }
}
