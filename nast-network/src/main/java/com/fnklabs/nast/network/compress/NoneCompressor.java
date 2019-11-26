package com.fnklabs.nast.network.compress;

import java.nio.ByteBuffer;

public class NoneCompressor implements Compressor {
    @Override
    public void compress(ByteBuffer data, ByteBuffer buf) {
        buf.put(data);
    }

    @Override
    public void uncompress(ByteBuffer buf, ByteBuffer data) {
        data.put(buf);
    }
}
