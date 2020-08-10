package com.fnklabs.nast.network;

import com.fnklabs.nast.network.io.ChannelClosedException;
import com.fnklabs.nast.network.io.Session;
import com.fnklabs.nast.network.io.WriteFuture;
import com.fnklabs.nast.network.io.WriteOperationQueueLimited;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Client channel handler
 */
public abstract class AbstractClientChannelHandler implements com.fnklabs.nast.network.io.ClientChannelHandler {

    private static final Logger log = LoggerFactory.getLogger(AbstractClientChannelHandler.class);
    private final ArrayBlockingQueue<WriteFuture> writeFutureQueue;
    private final AtomicBoolean connected = new AtomicBoolean(true);


    public AbstractClientChannelHandler(int writeFutureQueueSize) {
        writeFutureQueue = new ArrayBlockingQueue<>(writeFutureQueueSize);
    }

    @Override
    public WriteFuture onWrite(Session session) {
        return writeFutureQueue.poll();
    }

    @Override
    public void onDisconnect(Session session) {
        try {
            close();
        } catch (Exception e) {
            log.warn("Can't disconnect", e);
        }
    }

    @Override
    public void close() throws Exception {
        log.debug("closing channel...");

        if (connected.compareAndSet(true, false)) {
            while (!writeFutureQueue.isEmpty()) {
                WriteFuture writeFuture = writeFutureQueue.poll();

                log.debug("complete pending write operation");

                writeFuture.completeExceptionally(new ChannelClosedException(null));
            }
        }

        log.debug("channel was closed.");
    }

    @Override
    public WriteFuture send(ByteBuffer dataBuf) {
        WriteFuture writeFuture = new WriteFuture(dataBuf);

        if (!connected.get()) {
            writeFuture.completeExceptionally(new ChannelClosedException(null));
        } else {

            if (!writeFutureQueue.offer(writeFuture)) {
                LoggerFactory.getLogger(getClass()).warn("can't send data outgoing queue is full");
                writeFuture.completeExceptionally(new WriteOperationQueueLimited(String.format("remaining capacity is %d", writeFutureQueue.remainingCapacity())));
            }

            LoggerFactory.getLogger(getClass()).debug("put write future to outgoing queue {}", writeFutureQueue);
        }

        return writeFuture;
    }
}
