package com.fnklabs.nast.network.io;

import java.net.StandardSocketOptions;

public class TcpNoDelayOptionsConfigurer extends AbstractSocketOptionsConfigurer<Boolean> {

    public TcpNoDelayOptionsConfigurer(boolean value) {
        super(StandardSocketOptions.TCP_NODELAY, value);
    }
}
