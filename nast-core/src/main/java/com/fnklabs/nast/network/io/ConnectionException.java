package com.fnklabs.nast.network.io;

import com.google.common.net.HostAndPort;

/**
 * Thrown on any connection problems
 */
public class ConnectionException extends HostNotAvailableException {
    public ConnectionException(String message, HostAndPort address) {
        super(String.format("%s [%s]", message, address));
    }

    public ConnectionException(String message, HostAndPort address, Exception e) {
        super(String.format("%s [%s]", message, address), e);
    }
}
