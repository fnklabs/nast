package com.fnklabs.nast.network.io;

import java.net.StandardSocketOptions;

public class RcvBufferSocketOptionsConfigurer extends AbstractSocketOptionsConfigurer<Integer> {

    public RcvBufferSocketOptionsConfigurer(int length) {
        super(StandardSocketOptions.SO_RCVBUF, length);
    }
}