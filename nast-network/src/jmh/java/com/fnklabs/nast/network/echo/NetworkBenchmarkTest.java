package com.fnklabs.nast.network.echo;

import com.fnklabs.nast.network.Client;
import com.fnklabs.nast.network.Server;
import com.google.common.net.HostAndPort;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Threads(value = 4)
@Fork(value = 4, jvmArgs = {
        "-server",
        "-Xms512m",
        "-Xmx2G",
        "-XX:NewSize=512m",
        "-XX:SurvivorRatio=6",
        "-XX:+AlwaysPreTouch",
        "-XX:+UseG1GC",
        "-XX:MaxGCPauseMillis=2000",
        "-XX:GCTimeRatio=4",
        "-XX:InitiatingHeapOccupancyPercent=30",
        "-XX:G1HeapRegionSize=8M",
        "-XX:ConcGCThreads=8",
        "-XX:G1HeapWastePercent=10",
        "-XX:+UseTLAB",
        "-XX:+ScavengeBeforeFullGC",
        "-XX:+DisableExplicitGC",
})
@Warmup(iterations = 20, timeUnit = TimeUnit.MILLISECONDS)
public class NetworkBenchmarkTest {

    @Benchmark
    public void send(ClientContext clientContext, ServerContext serverContext) throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> replyFuture = new CompletableFuture<>();

        int id = clientContext.ID_COUNTER.incrementAndGet();

        ByteBuffer buffer = clientContext.getBuffer();

        buffer.putInt(id);
        buffer.putInt(id);

        ClientChannelHandler.REPLY_FUTURES.put(id, replyFuture);

        clientContext.client.write(buffer.array());

        buffer.rewind();

        Integer result = replyFuture.get();
    }


    @State(Scope.Benchmark)
    public static class ServerContext {
        public Server server;
        private HostAndPort remoteAddress = HostAndPort.fromString("127.0.0.1:10000");

        public static AtomicReference<Boolean> started = new AtomicReference<>(false);

        @Setup
        public void setUp() throws IOException {
            server = new Server(remoteAddress, new ServerEchoChannelHandler(), 4);

            started.set(true);
        }


        @TearDown
        public void tearDown() throws Exception {
            started.set(false);
            server.close();
        }
    }

    @State(Scope.Benchmark)
    public static class ClientContext {
        public Client client;
        private HostAndPort remoteAddress = HostAndPort.fromString("127.0.0.1:10000");
        public final AtomicInteger ID_COUNTER = new AtomicInteger();

        public final ThreadLocal<ByteBuffer> BUFFER = ThreadLocal.withInitial(() -> ByteBuffer.allocate(8));

        public ByteBuffer getBuffer() {
            return BUFFER.get();
        }

        @Setup
        public void setUp() throws IOException {
            while (ServerContext.started.get() == false) {

            }

            client = new Client(remoteAddress, new ClientChannelHandler());
        }


        @TearDown
        public void tearDown() throws Exception {
            client.close();
        }
    }
}
