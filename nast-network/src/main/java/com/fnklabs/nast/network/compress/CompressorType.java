package com.fnklabs.nast.network.compress;

import java.io.IOException;
import java.nio.ByteBuffer;

public enum CompressorType {
    NONE(new NoneCompressor()),
    SNAPPY(new SnappyCompressor());

    private final Compressor compressor;

    public static final CompressorType DEFAULT = NONE;

    CompressorType(Compressor compressor) {this.compressor = compressor;}

    public void compress(ByteBuffer data, ByteBuffer buffer) throws IOException {
        compressor.compress(data, buffer);
    }

    public void uncompress(ByteBuffer buffer, ByteBuffer data) throws IOException {
        compressor.uncompress(buffer, data);
    }

}
