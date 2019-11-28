package com.fnklabs.nast.network.benchmark.nast;

import com.fnklabs.nast.network.benchmark.nast.handler.ServerEchoChannelHandler;
import com.fnklabs.nast.network.io.ServerChannel;
import com.google.common.net.HostAndPort;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.util.concurrent.atomic.AtomicInteger;

@State(Scope.Benchmark)
public class NastServerContext {

    public static final int ATTEMPTS = 1000;
    public static final HostAndPort remoteAddress = HostAndPort.fromString("127.0.0.1:10000");
    public ServerChannel serverChannel;

    public final AtomicInteger ID_COUNTER = new AtomicInteger();


    @Setup
    public void setUp() throws Exception {
        serverChannel = new ServerChannel(remoteAddress, new ServerEchoChannelHandler(10_000), 4);
    }


    @TearDown
    public void tearDown() throws Exception {
        serverChannel.close();
    }
}
