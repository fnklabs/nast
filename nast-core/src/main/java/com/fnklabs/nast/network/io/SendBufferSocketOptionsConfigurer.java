package com.fnklabs.nast.network.io;

import java.net.StandardSocketOptions;

public class SendBufferSocketOptionsConfigurer extends AbstractSocketOptionsConfigurer<Integer> {

    public SendBufferSocketOptionsConfigurer(int length) {
        super(StandardSocketOptions.SO_SNDBUF, length);
    }
}
