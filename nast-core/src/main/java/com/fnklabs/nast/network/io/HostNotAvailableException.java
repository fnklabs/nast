package com.fnklabs.nast.network.io;

public class HostNotAvailableException extends NetworkException {
    public HostNotAvailableException() {
        super();
    }

    public HostNotAvailableException(String message) {
        super(message);
    }

    public HostNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public HostNotAvailableException(Throwable cause) {
        super(cause);
    }

    protected HostNotAvailableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
