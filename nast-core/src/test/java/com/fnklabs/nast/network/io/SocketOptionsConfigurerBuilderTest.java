package com.fnklabs.nast.network.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.channels.NetworkChannel;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SocketOptionsConfigurerBuilderTest {
    @Mock
    private NetworkChannel networkChannel;

    @Test
    void tcpNoDelay() {
        assertAll(
                () -> {
                    SocketOptionsConfigurer configurer = SocketOptionsConfigurerBuilder.builder()
                                                                                       .tcpNoDelay(true)
                                                                                       .build();

                    configurer.apply(networkChannel);

                    verify(networkChannel).setOption(StandardSocketOptions.TCP_NODELAY, true);
                },
                () -> {
                    SocketOptionsConfigurer configurer = SocketOptionsConfigurerBuilder.builder()
                                                                                       .tcpNoDelay(false)
                                                                                       .build();

                    configurer.apply(networkChannel);

                    verify(networkChannel).setOption(StandardSocketOptions.TCP_NODELAY, false);
                }
        );
    }

    @Test
    void keepAlive() {
        assertAll(
                () -> {
                    SocketOptionsConfigurer configurer = SocketOptionsConfigurerBuilder.builder()
                                                                                       .keepAlive(true)
                                                                                       .build();

                    configurer.apply(networkChannel);

                    verify(networkChannel).setOption(StandardSocketOptions.SO_KEEPALIVE, true);
                },
                () -> {
                    SocketOptionsConfigurer configurer = SocketOptionsConfigurerBuilder.builder()
                                                                                       .keepAlive(false)
                                                                                       .build();

                    configurer.apply(networkChannel);

                    verify(networkChannel).setOption(StandardSocketOptions.SO_KEEPALIVE, false);
                }
        );
    }

    @Test
    void rcvBuffer() {
        assertAll(
                () -> {
                    SocketOptionsConfigurer configurer = SocketOptionsConfigurerBuilder.builder()
                                                                                       .rcvBuffer(64)
                                                                                       .build();

                    configurer.apply(networkChannel);

                    verify(networkChannel).setOption(StandardSocketOptions.SO_RCVBUF, 64);
                },
                () -> {
                    SocketOptionsConfigurer configurer = SocketOptionsConfigurerBuilder.builder()
                                                                                       .rcvBuffer(65)
                                                                                       .build();

                    configurer.apply(networkChannel);

                    verify(networkChannel).setOption(StandardSocketOptions.SO_RCVBUF, 65);
                }
        );
    }

    @Test
    void sndBuffer() {
        assertAll(
                () -> {
                    SocketOptionsConfigurer configurer = SocketOptionsConfigurerBuilder.builder()
                                                                                       .sndBuffer(64)
                                                                                       .build();

                    configurer.apply(networkChannel);

                    verify(networkChannel).setOption(StandardSocketOptions.SO_SNDBUF, 64);
                },
                () -> {
                    SocketOptionsConfigurer configurer = SocketOptionsConfigurerBuilder.builder()
                                                                                       .sndBuffer(65)
                                                                                       .build();

                    configurer.apply(networkChannel);

                    verify(networkChannel).setOption(StandardSocketOptions.SO_SNDBUF, 65);
                }
        );
    }

    @Test
    void defaultConfigurer() throws IOException {
        SocketOptionsConfigurer configurer = SocketOptionsConfigurerBuilder.builder()
                                                                           .build();

        configurer.apply(networkChannel);

        verify(networkChannel).setOption(StandardSocketOptions.TCP_NODELAY, true);
        verify(networkChannel).setOption(StandardSocketOptions.SO_KEEPALIVE, true);
        verify(networkChannel).setOption(StandardSocketOptions.SO_SNDBUF, 1024);
        verify(networkChannel).setOption(StandardSocketOptions.SO_RCVBUF, 1024);
    }
}