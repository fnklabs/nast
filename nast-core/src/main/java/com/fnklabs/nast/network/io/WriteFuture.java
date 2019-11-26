package com.fnklabs.nast.network.io;


import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * Future for writing data to {@link java.nio.channels.SocketChannel}
 */
public class WriteFuture extends CompletableFuture<Void> {
    private final ByteBuffer buffer;
    private boolean encoded = false;

    public WriteFuture(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    boolean isEncoded() {
        return encoded;
    }

    void setEncoded(boolean encoded) {
        this.encoded = encoded;
    }
}
