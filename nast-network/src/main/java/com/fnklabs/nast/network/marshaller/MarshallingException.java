package com.fnklabs.nast.network.marshaller;

import com.fnklabs.nast.network.io.NetworkException;

public class MarshallingException extends NetworkException {
    public MarshallingException() {
    }

    public MarshallingException(String message) {
        super(message);
    }

    public MarshallingException(String message, Throwable cause) {
        super(message, cause);
    }

    public MarshallingException(Throwable cause) {
        super(cause);
    }

    public MarshallingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
