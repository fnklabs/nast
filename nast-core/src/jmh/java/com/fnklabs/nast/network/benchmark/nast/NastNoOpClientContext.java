package com.fnklabs.nast.network.benchmark.nast;

import com.fnklabs.nast.network.benchmark.nast.handler.ClientNoOpHandler;
import com.fnklabs.nast.network.io.ClientChannel;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

public class NastNoOpClientContext {

    public ClientChannel clientChannel;

    public void setUp(NastNoOpServerContext context) throws Exception {
        clientChannel = new ClientChannel(context.remoteAddress, new ClientNoOpHandler(10_000));
    }

    public void tearDown() throws Exception {
        clientChannel.close();
    }

    @State(Scope.Benchmark)
    public static class ScopeBenchmark extends NastNoOpClientContext {
        @Setup
        @Override
        public void setUp(NastNoOpServerContext context) throws Exception {
            super.setUp(context);
        }

        @TearDown
        @Override
        public void tearDown() throws Exception {
            super.tearDown();
        }
    }

    @State(Scope.Thread)
    public static class ScopeThread extends NastNoOpClientContext {
        @Setup
        @Override
        public void setUp(NastNoOpServerContext context) throws Exception {
            super.setUp(context);
        }

        @TearDown
        @Override
        public void tearDown() throws Exception {
            super.tearDown();
        }
    }
}
