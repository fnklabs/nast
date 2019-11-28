package com.fnklabs.nast.network.io;

import java.nio.ByteBuffer;

/**
 * Could be thrown by {@link ClientChannelHandler#send(ByteBuffer)}
 */
public class WriteOperationQueueLimited extends NetworkException {
    public WriteOperationQueueLimited() {}

    public WriteOperationQueueLimited(String message) {
        super(message);
    }
}
