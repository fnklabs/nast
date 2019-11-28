package com.fnklabs.nast.network.benchmark.nast;

import com.fnklabs.nast.network.benchmark.nast.handler.ClientChannelHandler;
import com.fnklabs.nast.network.io.ClientChannel;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;


public class NastClientContext {

    public ClientChannel clientChannel;
    public ClientChannelHandler channelHandler;


    public void setUp(NastServerContext serverContext) throws Exception {
        channelHandler = new ClientChannelHandler(10_000);
        clientChannel = new ClientChannel(NastServerContext.remoteAddress, channelHandler);
    }


    public void tearDown() throws Exception {
        clientChannel.close();
    }

    @State(Scope.Thread)
    public static class ScopeThread extends NastClientContext {
        @Setup
        @Override
        public void setUp(NastServerContext serverContext) throws Exception {
            super.setUp(serverContext);
        }

        @TearDown
        @Override
        public void tearDown() throws Exception {
            super.tearDown();
        }
    }

    @State(Scope.Benchmark)
    public static class ScopeBenchmark extends NastClientContext {
        @Setup
        @Override
        public void setUp(NastServerContext serverContext) throws Exception {
            super.setUp(serverContext);
        }

        @TearDown
        @Override
        public void tearDown() throws Exception {
            super.tearDown();
        }
    }
}
