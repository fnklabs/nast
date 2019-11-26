package com.fnklabs.nast.network.io;

import java.nio.ByteBuffer;

/**
 * Client channel handler interface that also support send operations from {@link ClientChannel}
 */
public interface ClientChannelHandler extends ChannelHandler {
    /**
     * Put data to outgoing queue
     *
     * @param dataBuf {@link ByteBuffer}
     *
     * @return Write future
     *
     * @throws WriteOperationQueueLimited if outgoing queue is full and data could be send
     */
    WriteFuture send(ByteBuffer dataBuf);
}
