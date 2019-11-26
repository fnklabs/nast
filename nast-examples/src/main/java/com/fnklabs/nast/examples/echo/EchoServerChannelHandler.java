package com.fnklabs.nast.examples.echo;

import com.fnklabs.nast.network.io.ChannelHandler;
import com.fnklabs.nast.network.io.Session;
import com.fnklabs.nast.network.io.WriteFuture;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class EchoServerChannelHandler implements ChannelHandler {
    private final Map<Long, Queue<WriteFuture>> clientOutgoingQueue = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<Void> onRead(Session session, ByteBuffer data) {
        String msg = StandardCharsets.UTF_8.decode(data).toString();

        System.out.println(String.format("retrieve %d > %s", session.getId(), msg));

        byte[] bytes = msg.getBytes();

        getOutQueue(session).offer(new WriteFuture(ByteBuffer.wrap(bytes)));

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public WriteFuture onWrite(Session session) {
        Queue<WriteFuture> outQueue = getOutQueue(session);

        return outQueue.poll();
    }

    Queue<WriteFuture> getOutQueue(Session session) {
        return clientOutgoingQueue.computeIfAbsent(session.getId(), id -> {
            return new ArrayBlockingQueue<>(100);
        });
    }

    @Override
    public void onDisconnect(Session session) {
        Queue<WriteFuture> outgoingQueue = clientOutgoingQueue.remove(session.getId());

        outgoingQueue.clear();

        System.out.println(String.format("%s disconnected", session));
    }
}
