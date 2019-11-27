package com.fnklabs.nast.network.io;

public class StopWorker extends NetworkException {
    public StopWorker() {
        super(null, null, true, false);
    }
}
