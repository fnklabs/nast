package com.fnklabs.nast.network.stream;

import com.fnklabs.nast.network.AbstractClientChannelHandler;
import com.fnklabs.nast.network.io.Session;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Client channel handler
 */
public class ClientChannelHandler extends AbstractClientChannelHandler {

    Map<Integer, CompletableFuture<Void>> streamFutures = new ConcurrentHashMap<>();
    Consumer<byte[]> streamItemConsumer;

    public ClientChannelHandler(int writeFutureQueueSize) {
        super(writeFutureQueueSize);
    }

    @Override
    public CompletableFuture<Void> onRead(Session session, ByteBuffer dataBuf) {
        int id = dataBuf.getInt();
        byte eol = dataBuf.get();

        if (eol == (byte) 1) {
            CompletableFuture<Void> future = streamFutures.remove(id);

            future.complete(null);

        } else {
            byte[] data = new byte[3];

            dataBuf.get(data);

            streamItemConsumer.accept(data);
        }


        return CompletableFuture.completedFuture(null);
    }
}
