package com.fnklabs.nast.network.echo;

import com.fnklabs.nast.network.AbstractServerChannelHandler;
import com.fnklabs.nast.network.io.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class ServerNoOpChannelHandler extends AbstractServerChannelHandler {

    public static final Logger log = LoggerFactory.getLogger(ServerNoOpChannelHandler.class);


    public ServerNoOpChannelHandler(int queueSize) {
        super(queueSize);
    }


    @Override
    public CompletableFuture<Void> onRead(Session session, ByteBuffer data) {
        int requestID = data.getInt();
        int value = data.getInt();


        return CompletableFuture.completedFuture(null);
    }


}
