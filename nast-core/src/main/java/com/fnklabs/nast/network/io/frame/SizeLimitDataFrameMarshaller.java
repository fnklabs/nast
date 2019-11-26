package com.fnklabs.nast.network.io.frame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Frame marshaller encode/decode frame with size header
 * <p>
 * frame consist of
 * <pre>
 * ---------------------
 * | 4 bytes | n bytes |
 * |--------------------
 * |  Header |  data   |
 * ---------------------
 * </pre>
 */
public class SizeLimitDataFrameMarshaller implements DataFrameMarshaller {
    private static final int DATA_SIZE_LENGTH = Integer.BYTES;

    private static final int HEADER_SIZE = DATA_SIZE_LENGTH;

    private static final Logger log = LoggerFactory.getLogger(SizeLimitDataFrameMarshaller.class);

    private static int getFrameSize(ByteBuffer msgBuffer) {
        return msgBuffer.getInt();
    }

    /**
     * Write frame length into frame buffer
     *
     * @param buffer     FrameBuffer
     * @param dataLength Data length in bytes
     */
    public static void writeHeader(ByteBuffer buffer, int dataLength) {
        buffer.putInt(dataLength + HEADER_SIZE);
    }

    @Override
    public ByteBuffer decode(ByteBuffer inBoundBuf) {
        if (inBoundBuf.remaining() >= HEADER_SIZE) {

            ByteBuffer frmBuf = inBoundBuf.slice();

            int frameSize = getFrameSize(frmBuf);
            int dataSize = frameSize - HEADER_SIZE;

            // todo check if frame size is greater than buffer capacity

            if (frmBuf.remaining() < frameSize - HEADER_SIZE) {
                log.debug("not enough data. expected {} actual {}", frameSize, frmBuf);

                return null;
            }

            inBoundBuf.position(inBoundBuf.position() + frameSize); // move buffer to another frame position

            frmBuf.limit(frmBuf.position() + dataSize);

            return frmBuf;
        }

        return null;
    }

    @Override
    public void encode(ByteBuffer dataBuf, ByteBuffer frameBuf) throws FrameException {
        log.debug("encoding frame. data: {} frame: {}", dataBuf, frameBuf);

        int dataSize = dataBuf.remaining();

        if (frameBuf.remaining() < HEADER_SIZE + dataSize) {
            throw new FrameException(String.format("can't encode frame. data is bigger then available buffer (%s > %s)", dataBuf, frameBuf));
        }

        log.debug("encoding frame. data: {} frame: {}", dataBuf, frameBuf);

        writeHeader(frameBuf, dataSize);
        frameBuf.put(dataBuf);

        log.debug("encode frame. frame: {}", frameBuf);
    }

}
