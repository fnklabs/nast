package com.fnklabs.nast.network.benchmark.nast;

import com.fnklabs.nast.network.AbstractBenchmark;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OperationsPerInvocation;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;


public class NastBenchmarkTest extends AbstractBenchmark {

    @OperationsPerInvocation(1000)
    @Benchmark
    public void requestReplySeveralClientsAsync(NastServerContext context, NastClientContext.ScopeThread clientContext) throws Exception {

        CountDownLatch countDownLatch = new CountDownLatch(NastServerContext.ATTEMPTS);

        for (int i = 0; i < NastServerContext.ATTEMPTS; i++) {
            int id = context.ID_COUNTER.incrementAndGet();

            ByteBuffer buffer = ByteBuffer.allocate(8);

            buffer.putInt(id);
            buffer.putInt(id);
            buffer.rewind();

            CompletableFuture<Integer> replyFuture = new CompletableFuture<>();

            clientContext.channelHandler.REPLY_FUTURES.put(id, replyFuture);

            clientContext.clientChannel.send(buffer);

            replyFuture.thenAccept(r -> countDownLatch.countDown());
        }


        countDownLatch.await();
    }

    @OperationsPerInvocation(1000)
    @Benchmark
    public void requestReplyOneClientAsync(NastServerContext context, NastClientContext.ScopeBenchmark clientContext) throws Exception {

        CountDownLatch countDownLatch = new CountDownLatch(NastServerContext.ATTEMPTS);

        for (int i = 0; i < NastServerContext.ATTEMPTS; i++) {
            int id = context.ID_COUNTER.incrementAndGet();

            ByteBuffer buffer = ByteBuffer.allocate(8);

            buffer.putInt(id);
            buffer.putInt(id);
            buffer.rewind();

            CompletableFuture<Integer> replyFuture = new CompletableFuture<>();

            clientContext.channelHandler.REPLY_FUTURES.put(id, replyFuture);

            clientContext.clientChannel.send(buffer);

            replyFuture.thenAccept(r -> countDownLatch.countDown());
        }


        countDownLatch.await();
    }


    @Benchmark
    public void requestReplySeveralClientsSync(NastServerContext context, NastClientContext.ScopeThread clientContext) throws Exception {
        int id = context.ID_COUNTER.incrementAndGet();

        ByteBuffer buffer = ByteBuffer.allocate(8);

        buffer.putInt(id);
        buffer.putInt(id);
        buffer.rewind();

        CompletableFuture<Integer> replyFuture = new CompletableFuture<>();

        clientContext.channelHandler.REPLY_FUTURES.put(id, replyFuture);

        clientContext.clientChannel.send(buffer);

        replyFuture.get();
    }

    @Benchmark
    public void requestReplyOneClientSync(NastServerContext context, NastClientContext.ScopeBenchmark clientContext) throws Exception {
        int id = context.ID_COUNTER.incrementAndGet();

        ByteBuffer buffer = ByteBuffer.allocate(8);

        buffer.putInt(id);
        buffer.putInt(id);
        buffer.rewind();

        CompletableFuture<Integer> replyFuture = new CompletableFuture<>();

        clientContext.channelHandler.REPLY_FUTURES.put(id, replyFuture);

        clientContext.clientChannel.send(buffer);

        replyFuture.get();
    }


    @OperationsPerInvocation(1000)
    @Benchmark
    public void requestNoReplySeveralClientsAsync(NastNoOpServerContext context, NastNoOpClientContext.ScopeThread clientContext) throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(NastServerContext.ATTEMPTS);

        for (int i = 0; i < NastNoOpServerContext.ATTEMPTS; i++) {
            int id = context.ID_COUNTER.incrementAndGet();

            ByteBuffer buffer = ByteBuffer.allocate(8);

            buffer.putInt(id);
            buffer.rewind();


            CompletableFuture<Void> senFuture = clientContext.clientChannel.send(buffer);

            senFuture.thenAccept(r -> countDownLatch.countDown());
        }

        countDownLatch.await();
    }

    @OperationsPerInvocation(1000)
    @Benchmark
    public void requestNoReplyOneClientAsync(NastNoOpServerContext context, NastNoOpClientContext.ScopeBenchmark clientContext) throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(NastServerContext.ATTEMPTS);

        for (int i = 0; i < NastNoOpServerContext.ATTEMPTS; i++) {
            int id = context.ID_COUNTER.incrementAndGet();

            ByteBuffer buffer = ByteBuffer.allocate(8);

            buffer.putInt(id);
            buffer.rewind();


            CompletableFuture<Void> senFuture = clientContext.clientChannel.send(buffer);

            senFuture.thenAccept(r -> countDownLatch.countDown());
        }

        countDownLatch.await();
    }

    @Benchmark
    public void requestNoReplySeveralClientsSync(NastNoOpServerContext context, NastNoOpClientContext.ScopeThread clientContext) throws Exception {

        int id = context.ID_COUNTER.incrementAndGet();

        ByteBuffer buffer = ByteBuffer.allocate(8);

        buffer.putInt(id);
        buffer.rewind();


        CompletableFuture<Void> senFuture = clientContext.clientChannel.send(buffer);

        senFuture.get();
    }

    @Benchmark
    public void requestNoReplyOneClientSync(NastNoOpServerContext context, NastNoOpClientContext.ScopeBenchmark clientContext) throws Exception {

        int id = context.ID_COUNTER.incrementAndGet();

        ByteBuffer buffer = ByteBuffer.allocate(8);

        buffer.putInt(id);
        buffer.rewind();


        CompletableFuture<Void> senFuture = clientContext.clientChannel.send(buffer);

        senFuture.get();
    }

}
