package com.fnklabs.nast.network.compress;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface Compressor {
    /**
     * Compress data from data buffer and write it to buf
     *
     * @param data Data buffer
     * @param buf  Output buffer
     *
     * @throws IOException if can't compress data
     */
    void compress(ByteBuffer data, ByteBuffer buf) throws IOException;

    /**
     * Uncompress data from buffer and write it to data buf
     *
     * @param buf  Input buffer
     * @param data Data buffer
     *
     * @throws IOException if can't uncompress data
     */
    void uncompress(ByteBuffer buf, ByteBuffer data) throws IOException;
}
