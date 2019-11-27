package com.fnklabs.nast.network.echo;

import com.fnklabs.nast.network.AbstractClientChannelHandler;
import com.fnklabs.nast.network.io.Session;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * Client channel handler
 */
public class ClientNoOpHandler extends AbstractClientChannelHandler {


    public ClientNoOpHandler(int writeFutureQueueSize) {
        super(writeFutureQueueSize);
    }

    @Override
    public CompletableFuture<Void> onRead(Session session, ByteBuffer data) {
        throw new IllegalStateException("retrieve unknown reply ");
    }
}
