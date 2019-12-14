package com.fnklabs.nast.network.io;

import java.net.StandardSocketOptions;

public class KeepAliveOptionsConfigurer extends AbstractSocketOptionsConfigurer<Boolean> {

    public KeepAliveOptionsConfigurer(boolean value) {
        super(StandardSocketOptions.SO_KEEPALIVE, value);
    }
}
