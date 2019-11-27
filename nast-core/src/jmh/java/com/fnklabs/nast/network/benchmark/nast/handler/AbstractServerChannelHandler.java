package com.fnklabs.nast.network.benchmark.nast.handler;

import com.fnklabs.nast.network.io.ChannelHandler;
import com.fnklabs.nast.network.io.Session;
import com.fnklabs.nast.network.io.WriteFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

abstract public class AbstractServerChannelHandler implements ChannelHandler {

    public static final Logger log = LoggerFactory.getLogger(AbstractServerChannelHandler.class);

    private final Map<Long, Queue<WriteFuture>> replyQueue;
    private final int maxClientQueueSize;

    public AbstractServerChannelHandler(int queueSize) {
        maxClientQueueSize = queueSize;
        replyQueue = new ConcurrentHashMap<>();
    }


    @Override
    public WriteFuture onWrite(Session session) {

        return getClientQueue(session).poll();
    }

    @Override
    public void close() throws Exception {
        replyQueue.forEach((sessionId, queue) -> {
            while (!queue.isEmpty()) {
                WriteFuture poll = queue.poll();

                poll.completeExceptionally(new Exception("handler was closed"));
            }
        });

        replyQueue.clear();
    }

    @Override
    public void onDisconnect(Session session) {
        Queue<WriteFuture> queue = replyQueue.remove(session.getId());
        queue.clear();
    }

    protected Queue<WriteFuture> getClientQueue(Session session) {
        return replyQueue.computeIfAbsent(session.getId(), id -> {
            return new ArrayBlockingQueue<>(maxClientQueueSize);
        });
    }
}
