package com.fnklabs.nast.network.benchmark.nast.handler;

import com.fnklabs.nast.network.io.Session;
import com.fnklabs.nast.network.io.WriteFuture;
import com.fnklabs.nast.network.io.WriteOperationQueueLimited;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

public class ServerEchoChannelHandler extends AbstractServerChannelHandler {

    public static final Logger log = LoggerFactory.getLogger(ServerEchoChannelHandler.class);


    public ServerEchoChannelHandler(int queueSize) {
        super(queueSize);
    }


    @Override
    public CompletableFuture<Void> onRead(Session session, ByteBuffer data) {
        int requestID = data.getInt();
        int value = data.getInt();

        log.debug("retrieve request {}: {}", requestID, value);

        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(requestID);
        buffer.putInt(value);
        buffer.flip();

        Queue<WriteFuture> queue = getClientQueue(session);

        WriteFuture writeFuture = new WriteFuture(buffer);

        if (!queue.offer(writeFuture)) {
            writeFuture.completeExceptionally(new WriteOperationQueueLimited());
        }

        return writeFuture;
    }


}
