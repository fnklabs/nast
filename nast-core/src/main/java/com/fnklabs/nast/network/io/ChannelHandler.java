package com.fnklabs.nast.network.io;

import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * New data handler interface that must be implemented to process channel events (Read, Write, Disconnect)
 */
public interface ChannelHandler extends AutoCloseable {

    /**
     * Handle read event to process new data from frame buf
     *
     * @param data    Input data
     * @param session Channel session
     *
     * @return future for read operations
     */
    CompletableFuture<Void> onRead(Session session, ByteBuffer data);

    /**
     * Handle write event to write new data through ovbesrver
     *
     * @param session Client session
     *
     * @return ByteBuffer to write or null of nothing to write
     */
    @Nullable
    WriteFuture onWrite(Session session);

    /**
     * Handle disconnect connect event
     *
     * @param session Session that was disconnected
     */
    void onDisconnect(Session session);

    /**
     * Handle channel IO exception
     *
     * @param e Exception that was occur and must be processed
     */
    default void onException(Throwable e) {}

    @Override
    default void close() throws Exception {}
}
