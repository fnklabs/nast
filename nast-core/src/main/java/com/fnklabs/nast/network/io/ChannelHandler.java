package com.fnklabs.nast.network.io;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * New data handler interface that must be implemented to process channel events (Read, Write, Disconnect)
 */
public interface ChannelHandler extends AutoCloseable {

    /**
     * Handle read event to process new data from frame buf
     *
     * @param data Input data
     *
     * @return Future for handing data and send reply to observer
     */
    CompletableFuture<Void> onRead(Session session, ByteBuffer data);

    /**
     * Handle write event to write new data through ovbesrver
     *
     * @return ByteBuffer to write or null of nothing to write
     */
    WriteFuture onWrite(Session session);

    void onDisconnect(Session session);

    @Override
    default void close() throws Exception {}
}
