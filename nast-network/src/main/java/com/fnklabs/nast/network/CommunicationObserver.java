package com.fnklabs.nast.network;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface CommunicationObserver {
    /**
     * Apply new message
     *
     * @param data data that must be send
     *
     * @return Write future
     */
    CompletableFuture<Void> onNext(ByteBuffer data);
}
