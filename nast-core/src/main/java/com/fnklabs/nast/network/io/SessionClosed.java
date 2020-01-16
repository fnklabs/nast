package com.fnklabs.nast.network.io;

public class SessionClosed extends NetworkException {

    public SessionClosed() {
        super(null, null, true, false);
    }
}
