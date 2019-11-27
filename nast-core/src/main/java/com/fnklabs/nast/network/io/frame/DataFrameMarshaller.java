package com.fnklabs.nast.network.io.frame;

import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

/**
 * Decode/Encode frame
 * Decode frame from {@link java.nio.channels.SocketChannel} buffer and emmit from it frames
 * <p>
 * Buffer could consist of stream of byte and contains part of frame or several frames, for example
 * <p>
 * buffer contains part of frame
 * <pre>
 * Let buffer size = 8 bytes
 * Frame size = 8 bytes
 * </pre>
 * <p>
 *
 * <pre>
 *     [ F1 ], [F1]
 * </pre>
 * <p>
 * Frame was not fully read from network IO, so frame could not be decoded and will decoded as soon as new data will appeared on socket and read
 *
 * </b></b>
 * <p>
 * <p>
 * buffer contains several part of frame
 * <pre>
 * Let buffer size = 16 bytes
 * Frame size = 8 bytes
 * </pre>
 *
 * <pre>
 *     [ F1, F2, F3 ], [ F3, F5, F6 ]
 * </pre>
 * <p>
 * In this case in buffer could be several frame or part of frame.
 * In this case expected that from buffer will be read buffer that could be fully read. This solulution give another problem when
 */
public interface DataFrameMarshaller {

    /**
     * Try to decode frames from inBoundBuf
     * All frames must be emitted to frameConsumer.
     * If frame could not be fully read from inBoundBuf then buffer must be set to actual state because it will be compacted
     *
     * @param inBoundBuf buffer that collect all incoming data from
     *
     * @return frame {@link ByteBuffer} or null of frame could not be read
     */
    @Nullable
    ByteBuffer decode(ByteBuffer inBoundBuf);

    /**
     * Encode dataBuf into dataBuf frame and write it to frameBuf
     *
     * @param dataBuf  Buffer that contains data
     * @param frameBuf FrameBuffer to which data must be written
     */
    void encode(ByteBuffer dataBuf, ByteBuffer frameBuf) throws FrameException;


    /**
     * Get frame size from frame buffer
     *
     * @param frameBuffer FrameBuffer
     *
     * @return size of buffer
     */
    int getFrameSize(ByteBuffer frameBuffer);

    /**
     * Get target frame size for provided data buffer
     *
     * @param dataBuffer FrameBuffer
     *
     * @return size of framebuffer
     */
    int getTargetFrameSize(ByteBuffer dataBuffer);

}
