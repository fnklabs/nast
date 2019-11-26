package com.fnklabs.nast.network;

import com.fnklabs.nast.network.io.Session;
import com.fnklabs.nast.network.io.WriteFuture;
import com.fnklabs.nast.network.io.WriteOperationQueueLimited;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Client channel handler
 */
public abstract class AbstractClientChannelHandler implements com.fnklabs.nast.network.io.ClientChannelHandler {

    private final ArrayBlockingQueue<WriteFuture> writeFutureQueue;

    public AbstractClientChannelHandler(int writeFutureQueueSize) {
        writeFutureQueue = new ArrayBlockingQueue<>(writeFutureQueueSize);
    }


    @Override
    public WriteFuture onWrite(Session session) {

        return writeFutureQueue.poll();
    }

    @Override
    public void onDisconnect(Session session) {

    }

    @Override
    public void close() throws Exception {

        while (!writeFutureQueue.isEmpty()) {
            WriteFuture poll = writeFutureQueue.poll();

            poll.completeExceptionally(new Exception("handler was closed"));
        }
    }

    @Override
    public WriteFuture send(ByteBuffer dataBuf) {
        WriteFuture writeFuture = new WriteFuture(dataBuf);

        if (writeFutureQueue.offer(writeFuture)) {
            writeFuture.completeExceptionally(new WriteOperationQueueLimited());
        }

        return writeFuture;
    }
}
