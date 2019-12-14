package com.fnklabs.nast.network.io;

import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketOption;
import java.nio.channels.NetworkChannel;
import java.util.Objects;

abstract class AbstractSocketOptionsConfigurer<T> implements SocketOptionsConfigurer {
    private SocketOptionsConfigurer socketOptionsConfigurer;

    private final SocketOption<T> socketOption;
    private final T value;

    protected AbstractSocketOptionsConfigurer(SocketOption<T> socketOption, T value) {
        this.socketOption = socketOption;
        this.value = value;
    }

    @Override
    public void apply(NetworkChannel networkChannel) {
        try {
            networkChannel.setOption(socketOption, value);
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).warn("can't set socket option {} to {}", socketOption, value, e);
        }

        if (socketOptionsConfigurer != null) {
            socketOptionsConfigurer.apply(networkChannel);
        }
    }

    @Override
    public SocketOptionsConfigurer andThen(SocketOptionsConfigurer socketOptionsConfigurer) {
        return this.socketOptionsConfigurer = socketOptionsConfigurer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractSocketOptionsConfigurer<?> that = (AbstractSocketOptionsConfigurer<?>) o;
        return Objects.equals(socketOption, that.socketOption) &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(socketOption, value);
    }
}
