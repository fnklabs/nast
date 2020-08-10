package com.fnklabs.nast.network;

import com.fnklabs.nast.network.echo.ClientChannelHandler;
import com.fnklabs.nast.network.echo.ServerEchoChannelHandler;
import com.fnklabs.nast.network.io.ChannelClosedException;
import com.fnklabs.nast.network.io.ClientChannel;
import com.fnklabs.nast.network.io.ServerChannel;
import com.fnklabs.nast.network.io.Session;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.Futures;
import com.sun.org.apache.regexp.internal.RE;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * Test that cover if connection is lost.
 *
 * <ul>
 *     <li>New send operations from client must be rejected</li>
 *     <li>Old (not completed) send operations must be completed with exception</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
public class Nast9Test {
    private ServerChannel serverChannel;
    private ClientChannel clientChannel;

    private HostAndPort hostAndPort;
    private int queueSize;
    private ClientChannelHandler channelHandler;
    private static final int REQUEST_ID = 2;

    @BeforeEach
    void setUp() throws Exception {
        hostAndPort = HostAndPort.fromString("127.0.0.1:10000");
        queueSize = 10;

        serverChannel = new ServerChannel(hostAndPort, new ServerEchoChannelHandler(10), 1);
        channelHandler = Mockito.spy(new ClientChannelHandler(queueSize));
        clientChannel = new ClientChannel(hostAndPort, channelHandler);
    }

    @AfterEach
    void tearDown() throws Exception {
        clientChannel.close();
        serverChannel.close();
    }

    @Test
    public void canNotSendOnClosedConnection() throws Exception {
        serverChannel.close();

        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(REQUEST_ID);
        buffer.putInt(REQUEST_ID);
        buffer.flip();

        CompletableFuture<ByteBuffer> secondMessageFuture = new CompletableFuture<>();
        channelHandler.REPLY_FUTURES.put(REQUEST_ID, secondMessageFuture);

        CompletableFuture<Void> sendFuture = clientChannel.send(buffer);

        Assertions.assertThrows(Exception.class, () -> {
            sendFuture.get();
        });
        Assertions.assertThrows(Exception.class, () -> {
            Futures.getUnchecked(secondMessageFuture);
        });
    }

    /**
     * Processing pending replies is not covered by client, this logic is delegated to {@link com.fnklabs.nast.network.io.ChannelHandler}. So by this test must
     * be Verified that {@link com.fnklabs.nast.network.io.ChannelHandler#onDisconnect(Session)} is called on socket disconnect
     */
    @Test
    public void completePendingOperationsWithException() throws Exception {

        CompletableFuture<ByteBuffer> firstMessageFuture = new CompletableFuture<>();

        channelHandler.REPLY_FUTURES.put(1, firstMessageFuture);

        serverChannel.close();

        Assertions.assertThrows(Exception.class, () -> {
            firstMessageFuture.get();
        });

        verify(channelHandler).onDisconnect(any(Session.class));
    }
}
