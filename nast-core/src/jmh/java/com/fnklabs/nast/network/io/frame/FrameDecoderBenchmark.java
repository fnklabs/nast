package com.fnklabs.nast.network.io.frame;

import com.fnklabs.nast.network.AbstractBenchmark;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class FrameDecoderBenchmark extends AbstractBenchmark {


    @Benchmark
    public void encodeFrame(EncodeThreadContext threadContext, BenchmarkContext benchmarkContext) {
        benchmarkContext.frameDecoder.encode(threadContext.dataBuffer, threadContext.frmBuffer);

        threadContext.dataBuffer.rewind();
        threadContext.frmBuffer.clear();
    }

    @Benchmark
    public void decodeFrame(ThreadContext threadContext, BenchmarkContext benchmarkContext) throws ExecutionException, InterruptedException {
        benchmarkContext.frameDecoder.decode(threadContext.frmBuffer);

        threadContext.frmBuffer.rewind();
    }


    @State(Scope.Thread)
    public static class ThreadContext {
        public ByteBuffer frmBuffer;

        @Setup
        public void setUp() throws IOException {
            frmBuffer = ByteBuffer.allocateDirect(1024);

            SizeLimitDataFrameMarshaller dataFrameDecoder = new SizeLimitDataFrameMarshaller();

            dataFrameDecoder.writeHeader(frmBuffer, 1020);

            frmBuffer.put(new byte[1020]);

            frmBuffer.flip();
        }


        @TearDown
        public void tearDown() throws Exception {

        }
    }


    @State(Scope.Thread)
    public static class EncodeThreadContext {
        public ByteBuffer frmBuffer;
        public ByteBuffer dataBuffer;


        @Setup
        public void setUp() throws IOException {
            dataBuffer = ByteBuffer.allocateDirect(1020);
            frmBuffer = ByteBuffer.allocateDirect(1024);

            SizeLimitDataFrameMarshaller dataFrameDecoder = new SizeLimitDataFrameMarshaller();

            dataBuffer.put(new byte[1020]);
            dataBuffer.flip();
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
