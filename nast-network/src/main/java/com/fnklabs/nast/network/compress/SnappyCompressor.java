package com.fnklabs.nast.network.compress;

import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.nio.ByteBuffer;

public class SnappyCompressor implements Compressor {
    @Override
    public void compress(ByteBuffer data, ByteBuffer buf) throws IOException {
        ByteBuffer directBuf = ByteBuffer.allocateDirect(data.remaining());
        directBuf.put(data);
        directBuf.flip();

        int compress = Snappy.compress(directBuf, buf);
    }

    @Override
    public void uncompress(ByteBuffer buf, ByteBuffer data) throws IOException {
        Snappy.uncompress(buf, data);
    }
}
