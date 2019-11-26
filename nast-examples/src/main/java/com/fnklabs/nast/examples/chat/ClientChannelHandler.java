package com.fnklabs.nast.examples.chat;

import com.fnklabs.nast.network.io.Session;
import com.fnklabs.nast.network.io.WriteFuture;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;

public class ClientChannelHandler implements com.fnklabs.nast.network.io.ClientChannelHandler {
    private final ArrayBlockingQueue<WriteFuture> writeFutureQueue = new ArrayBlockingQueue<>(100);

    @Override
    public WriteFuture send(ByteBuffer dataBuf) {
        WriteFuture writeFuture = new WriteFuture(dataBuf);

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
        writeFutureQueue.clear();
    }
}
