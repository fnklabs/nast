package com.fnklabs.nast.network.io;

import java.nio.channels.SocketChannel;

/**
 * Exception that thrown if channel was closed
 */
public class ChannelClosedException extends NetworkException {
    /**
     * Closed socket channel
     */
    private final SocketChannel socketChannel;

    public ChannelClosedException(SocketChannel socketChannel) {this.socketChannel = socketChannel;}

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }
}
