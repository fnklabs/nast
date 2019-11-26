package com.fnklabs.nast.network;

import com.fnklabs.nast.network.io.ChannelHandler;
import com.fnklabs.nast.network.io.ServerChannel;
import com.fnklabs.nast.network.io.Session;
import com.fnklabs.nast.network.io.WriteFuture;
import com.google.common.net.HostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class Server implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(Server.class);

    private final ServerChannel serverChannel;

    /**
     * Initialize network server but does not start it
     */
    public Server(HostAndPort listenAddress, int connectorPoolSize) throws IOException {
        this.serverChannel = new ServerChannel(
                listenAddress,
                new ChannelHandler() {
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


                },
                connectorPoolSize
        );
    }

    @Override
    public void close() throws Exception {
        log.info("closing server");

        serverChannel.close();
    }


}