package com.fnklabs.nast.network.io;

import java.nio.channels.NetworkChannel;

/**
 * Configuration for NetworkChannel to change socketOptions
 */
public interface SocketOptionsConfigurer {
    void apply(NetworkChannel networkChannel);

    SocketOptionsConfigurer andThen(SocketOptionsConfigurer socketOptionsConfigurer);
}
