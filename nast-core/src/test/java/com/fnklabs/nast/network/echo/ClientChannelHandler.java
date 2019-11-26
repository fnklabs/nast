package com.fnklabs.nast.network.echo;

import com.fnklabs.nast.network.AbstractClientChannelHandler;
import com.fnklabs.nast.network.io.Session;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client channel handler
 */
public class ClientChannelHandler extends AbstractClientChannelHandler {

    public final Map<Integer, CompletableFuture<Integer>> REPLY_FUTURES = new ConcurrentHashMap<>();


    public ClientChannelHandler(int writeFutureQueueSize) {
        super(writeFutureQueueSize);
    }

    @Override
    public CompletableFuture<Void> onRead(Session session, ByteBuffer data) {
        int requestId = data.getInt();
        int value = data.getInt();

        LoggerFactory.getLogger(getClass()).debug("retrieve response from server {}: {}", requestId, value);

        CompletableFuture<Integer> reply = REPLY_FUTURES.remove(requestId);

        if (reply != null) {
            reply.complete(value);
        } else {
            throw new IllegalStateException("retrieve unknown reply " + requestId + ": " + value);
        }

        return CompletableFuture.completedFuture(null);
    }
}
