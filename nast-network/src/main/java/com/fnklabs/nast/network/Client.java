package com.fnklabs.nast.network;

import com.fnklabs.nast.network.io.ClientChannel;
import com.fnklabs.nast.network.io.ClientChannelHandler;
import com.fnklabs.nast.network.io.Session;
import com.fnklabs.nast.network.io.WriteFuture;
import com.google.common.net.HostAndPort;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class Client implements AutoCloseable {
    /**
     * Client connector instance
     */
    private final ClientChannel channel;

    private final Map<Long, CompletableFuture<Void>> sendFutures = new ConcurrentHashMap<>();


    public Client(HostAndPort remoteAddress) {
        channel = new ClientChannel(remoteAddress, new ClientChannelHandler() {


            @Override
            public WriteFuture send(ByteBuffer dataBuf) {
                return null;
            }

            @Override
            public CompletableFuture<Void> onRead(Session session, ByteBuffer data) {
                return null;
            }

            @Override
            public WriteFuture onWrite(Session session) {
                return null;
            }


            @Override
            public void onDisconnect(Session session) {

            }


        });
    }


    @Override
    public void close() throws Exception {
        channel.close();
    }
}
