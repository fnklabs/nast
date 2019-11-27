package com.fnklabs.nast.network.benchmark.netty;

import com.google.common.net.HostAndPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.ByteBuffer.allocateDirect;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ServerTest {

    private NettyServer nettyServer;
    private HostAndPort address;

    @BeforeEach
    public void setUp() throws Exception {
        address = HostAndPort.fromString("127.0.0.1:10000");
        nettyServer = new NettyServer(address, new ReplyHandler(), 1, 4);

    }

    @AfterEach
    public void tearDown() throws Exception {
        nettyServer.close();
    }

    @Test
    public void sync() throws Exception {
        try (NettyClient client = new NettyClient(address)) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(12);
            for (int i = 0; i < 1000; i++) {
                buffer.putInt(i);
                buffer.putInt(i);
                buffer.putInt(i);
                buffer.rewind();

                CompletableFuture<Integer> write = client.send(i, buffer);
                Integer integer = write.get();

                assertEquals(i, integer.intValue());
            }
        }
    }

    @Test
    public void async() throws Exception {

        ExecutorService executorService = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(4);


        AtomicInteger id = new AtomicInteger();

        for (int j = 0; j < 4; j++) {
            executorService.submit(() -> {
                try (NettyClient client = new NettyClient(address)) {
                    for (int i = 0; i < 1000; i++) {
                        int seq = id.incrementAndGet();

                        ByteBuffer buffer = allocateDirect(12);
                        buffer.putInt(seq);
                        buffer.putInt(seq);
                        buffer.putInt(seq);
                        buffer.rewind();

                        CompletableFuture<Integer> write = client.send(seq, buffer);
                        Integer integer = write.get();

                        assertEquals(seq, integer.intValue());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                latch.countDown();
            });
        }

        latch.await();

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.SECONDS);
    }
}
