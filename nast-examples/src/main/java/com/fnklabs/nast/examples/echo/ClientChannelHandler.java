package com.fnklabs.nast.examples.echo;

import com.fnklabs.nast.network.io.ChannelClosedException;
import com.fnklabs.nast.network.io.Session;
import com.fnklabs.nast.network.io.WriteFuture;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientChannelHandler implements com.fnklabs.nast.network.io.ClientChannelHandler {
    private final ArrayBlockingQueue<WriteFuture> writeFutureQueue = new ArrayBlockingQueue<>(100);
    private final AtomicBoolean connected = new AtomicBoolean(true);

    @Override
    public WriteFuture send(ByteBuffer dataBuf) {
        WriteFuture writeFuture = new WriteFuture(dataBuf);
        if (!connected.get()) {
            writeFuture.completeExceptionally(new ChannelClosedException(null)); // todo remove it

            return writeFuture;
        }

        writeFutureQueue.offer(writeFuture);

        return writeFuture;
    }

    @Override
    public CompletableFuture<Void> onRead(Session session, ByteBuffer data) {
        String msg = StandardCharsets.UTF_8.decode(data).toString();

        System.out.println(String.format("retrieve %d > %s", session.getId(), msg));

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public WriteFuture onWrite(Session session) {
        return writeFutureQueue.poll();
    }

    @Override
    public void onDisconnect(Session session) {
        if (connected.compareAndSet(true, false)) {

            for (WriteFuture writeFuture : writeFutureQueue) {
                writeFuture.completeExceptionally(new ChannelClosedException(null)); // todo change exception type
            }

            writeFutureQueue.clear();
        }
    }
}
