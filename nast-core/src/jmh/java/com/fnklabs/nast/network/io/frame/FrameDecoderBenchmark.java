package com.fnklabs.nast.network.io.frame;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Threads(value = 4)
@Fork(value = 1, jvmArgs = {
        "-server",
        "-Xms512m",
        "-Xmx2G",
        "-XX:NewSize=512m",
        "-XX:SurvivorRatio=6",
        "-XX:+AlwaysPreTouch",
        "-XX:+UseG1GC",
        "-XX:MaxGCPauseMillis=100",
        "-XX:GCTimeRatio=4",
        "-XX:InitiatingHeapOccupancyPercent=30",
        "-XX:G1HeapRegionSize=8M",
        "-XX:ConcGCThreads=4",
        "-XX:G1HeapWastePercent=10",
        "-XX:+UseTLAB",
        "-XX:+ScavengeBeforeFullGC",
        "-XX:+DisableExplicitGC",
})
@Warmup(iterations = 5, timeUnit = TimeUnit.MILLISECONDS)
public class FrameDecoderBenchmark {


    @Benchmark
    public void decode(ThreadContext threadContext, BenchmarkContext benchmarkContext) throws ExecutionException, InterruptedException {
        benchmarkContext.frameDecoder.decode(threadContext.buffer);

        threadContext.buffer.rewind();
    }


    @State(Scope.Thread)
    public static class ThreadContext {
        public ByteBuffer buffer;


        @Setup
        public void setUp() throws IOException {
            buffer = ByteBuffer.allocateDirect(4 + 1024);

            SizeLimitDataFrameMarshaller dataFrameDecoder = new SizeLimitDataFrameMarshaller();

            dataFrameDecoder.writeHeader(buffer, 1024);

            buffer.put(new byte[1024]);

            buffer.flip();
        }


        @TearDown
        public void tearDown() throws Exception {

        }
    }

    @State(Scope.Benchmark)
    public static class BenchmarkContext {
        public SizeLimitDataFrameMarshaller frameDecoder;

        public Consumer<ByteBuffer> frmConsumer;

        @Setup
        public void setUp() throws IOException {
            frameDecoder = new SizeLimitDataFrameMarshaller();
            frmConsumer = frm -> {};
        }


        @TearDown
        public void tearDown() throws Exception {

        }
    }
}
