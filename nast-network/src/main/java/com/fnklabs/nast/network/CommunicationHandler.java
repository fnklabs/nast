package com.fnklabs.nast.network;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public interface CommunicationHandler {
    /**
     * Handle and process new data from buffer
     *
     * @param inBuf     Income data buffer. buffer must be copied and released before returning back control
     * @param observer  Observer to apply reply data
     *
     * @return Future for handing data and send reply to observer
     */
    CompletableFuture<Void> handle(ByteBuffer inBuf, CommunicationObserver observer);
}
