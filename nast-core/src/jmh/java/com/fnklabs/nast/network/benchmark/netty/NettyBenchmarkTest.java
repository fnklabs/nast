package com.fnklabs.nast.network.benchmark.netty;

import com.fnklabs.nast.network.AbstractBenchmark;
import com.google.common.net.HostAndPort;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;


public class NettyBenchmarkTest extends AbstractBenchmark {

    @Benchmark
    public void nettyOneClientSync(NettyServerContext nettyServerContext, ClientStateBenchmark clientContext) throws Exception {

        int id = nettyServerContext.ID_COUNTER.incrementAndGet();

        ByteBuffer buffer = ByteBuffer.allocate(12);

        buffer.putInt(id);
        buffer.putInt(id);
        buffer.putInt(id);
        buffer.rewind();

        CompletableFuture<Integer> future = clientContext.networkClient.send(id, buffer);
        Integer result = future.get();
    }

    @Benchmark
    public void nettySeveralClientsSync(NettyServerContext nettyServerContext, ClientStateThread clientContext) throws Exception {

        int id = nettyServerContext.ID_COUNTER.incrementAndGet();

        ByteBuffer buffer = ByteBuffer.allocate(12);

        buffer.putInt(id);
        buffer.putInt(id);
        buffer.putInt(id);
        buffer.rewind();

        CompletableFuture<Integer> future = clientContext.networkClient.send(id, buffer);
        Integer result = future.get();
    }

    @OperationsPerInvocation(1_000)
    @Benchmark
    public void nettyOneClientAsync(NettyServerContext nettyServerContext, ClientStateBenchmark clientContext) throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1000);

        for (int i = 0; i < 1000; i++) {
            int id = nettyServerContext.ID_COUNTER.incrementAndGet();

            ByteBuffer buffer = ByteBuffer.allocate(12);

            buffer.putInt(id);
            buffer.putInt(id);
            buffer.putInt(id);
            buffer.rewind();

            CompletableFuture<Integer> future = clientContext.networkClient.send(id, buffer);
            future.thenAccept(r -> countDownLatch.countDown());
        }
        countDownLatch.await();
    }

    @OperationsPerInvocation(1_000)
    @Benchmark
    public void nettySeveralClientAsync(NettyServerContext nettyServerContext, ClientStateBenchmark clientContext) throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1000);

        for (int i = 0; i < 1000; i++) {
            int id = nettyServerContext.ID_COUNTER.incrementAndGet();

            ByteBuffer buffer = ByteBuffer.allocate(12);

            buffer.putInt(id);
            buffer.putInt(id);
            buffer.putInt(id);
            buffer.rewind();

            CompletableFuture<Integer> future = clientContext.networkClient.send(id, buffer);
            future.thenAccept(r -> countDownLatch.countDown());
        }
        countDownLatch.await();
    }

    @State(Scope.Benchmark)
    public static class NettyServerContext {
        private HostAndPort remoteAddress = HostAndPort.fromString("127.0.0.1:10000");

        public NettyServer networkServer;

        public final AtomicInteger ID_COUNTER = new AtomicInteger();

        @Setup
        public void setUp() throws Exception {
            networkServer = new NettyServer(remoteAddress, new ReplyHandler(), 1, 4);
        }


        @TearDown
        public void tearDown() throws Exception {
            networkServer.close();
        }
    }

    public static class NettyClientContext {

        public NettyClient networkClient;

        public void setUp(NettyServerContext serverContext) throws Exception {
            networkClient = new NettyClient(serverContext.remoteAddress);
        }


        public void tearDown() throws Exception {
            networkClient.close();
        }
    }

    @State(Scope.Thread)
    public static class ClientStateThread extends NettyClientContext {
        @Setup
        @Override
        public void setUp(NettyServerContext serverContext) throws Exception {
            super.setUp(serverContext);
        }

        @TearDown
        @Override
        public void tearDown() throws Exception {
            super.tearDown();
        }
    }

    @State(Scope.Benchmark)
    public static class ClientStateBenchmark extends NettyClientContext {
        @Setup
        @Override
        public void setUp(NettyServerContext serverContext) throws Exception {
            super.setUp(serverContext);
        }

        @TearDown
        @Override
        public void tearDown() throws Exception {
            super.tearDown();
        }
    }

}
