package com.fnklabs.nast.network.echo;

import com.fnklabs.nast.network.AbstractServerChannelHandler;
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
        ByteBuffer respBuf = ByteBuffer.allocate(data.remaining());

        respBuf.put(data);
        respBuf.flip();

        Queue<WriteFuture> queue = getClientQueue(session);

        WriteFuture writeFuture = new WriteFuture(respBuf);

        if (!queue.offer(writeFuture)) {
            writeFuture.completeExceptionally(new WriteOperationQueueLimited());
        }


        return writeFuture.thenApply(r -> null);
    }


}
