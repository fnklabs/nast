package com.fnklabs.nast.network.io.frame;

import com.fnklabs.nast.network.io.NetworkException;

import java.nio.ByteBuffer;

/**
 * Exception that could be thrown by {@link DataFrameMarshaller} on {@link DataFrameMarshaller#encode(ByteBuffer, ByteBuffer)}
 * {@link DataFrameMarshaller#decode(ByteBuffer)} error
 */
public class FrameException extends NetworkException {
    public FrameException() {
    }

    public FrameException(String message) {
        super(message);
    }

    public FrameException(String message, Throwable cause) {
        super(message, cause);
    }

    public FrameException(Throwable cause) {
        super(cause);
    }

    public FrameException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
