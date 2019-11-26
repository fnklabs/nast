package com.fnklabs.nast.network.stream;

import com.fnklabs.nast.network.AbstractServerChannelHandler;
import com.fnklabs.nast.network.io.Session;
import com.fnklabs.nast.network.io.WriteFuture;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

class StreamChannelHandler extends AbstractServerChannelHandler {


    StreamChannelHandler(int queueSize) {
        super(queueSize);
    }

    @Override
    public CompletableFuture<Void> onRead(Session session, ByteBuffer data) {
        int id = data.getInt();
        byte[] value = new byte[3];
        data.get(value);


        for (int i = 0; i < 100; i++) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(8);
            byteBuffer.putInt(id);
            byteBuffer.put((byte) 0);
            byteBuffer.put(value);

            byteBuffer.rewind();

            getClientQueue(session).offer(new WriteFuture(byteBuffer));
        }
        //


        ByteBuffer eolBuff = ByteBuffer.allocate(5);
        eolBuff.putInt(id);
        eolBuff.put((byte) 1);

        eolBuff.rewind();

        getClientQueue(session).offer(new WriteFuture(eolBuff));

        return CompletableFuture.completedFuture(null);
    }


}
