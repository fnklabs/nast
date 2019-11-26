package com.fnklabs.nast.network.stream;

import com.fnklabs.nast.network.io.ClientChannel;
import com.fnklabs.nast.network.io.ServerChannel;
import com.google.common.net.HostAndPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ServerCommunicatorStreamTest {
    private static final Logger log = LoggerFactory.getLogger(ServerCommunicatorStreamTest.class);

    private ServerChannel server;

    private HostAndPort hostAndPort = HostAndPort.fromParts("127.0.0.1", 10_000);
    private ClientChannel client;


    private ByteBuffer buffer;
    private ClientChannelHandler channelHandler;

    @BeforeEach
    public void setUp() throws Exception {
        buffer = ByteBuffer.allocate(4 + 3);
        server = new ServerChannel(hostAndPort, new StreamChannelHandler(1_000), 4);

        channelHandler = new ClientChannelHandler(100);
        client = new ClientChannel(hostAndPort, channelHandler);

    }

    @AfterEach
    public void tearDown() throws Exception {
        client.close();
        server.close();
    }


    @Test
    public void test() throws Exception {

        int msgCount = 5;

        byte[] data = new byte[]{1, 2, 3};


        for (int j = 0; j < msgCount; j++) {
            buffer.putInt(j);
            buffer.put(data);
            buffer.flip();

            AtomicInteger counts = new AtomicInteger();

            channelHandler.streamItemConsumer = bytes -> {
                counts.incrementAndGet();

                assertArrayEquals(data, bytes);
            };

            CompletableFuture<Void> streamFuture = new CompletableFuture<>();

            channelHandler.streamFutures.put(j, streamFuture);


            client.send(buffer);

            streamFuture.get();

            assertEquals(100, counts.get());

            buffer.rewind();
        } ;

    }
}