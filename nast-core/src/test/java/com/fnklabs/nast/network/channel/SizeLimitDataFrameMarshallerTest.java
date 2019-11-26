package com.fnklabs.nast.network.channel;

import com.fnklabs.nast.network.io.frame.SizeLimitDataFrameMarshaller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SizeLimitDataFrameMarshallerTest {

    public static final int BUFFER_SIZE = 64;

    private SizeLimitDataFrameMarshaller sizeLimitFrameDecoder;


    private ByteBuffer buffer;


    @BeforeEach
    void setUp() {
        buffer = ByteBuffer.allocate(BUFFER_SIZE);

        sizeLimitFrameDecoder = new SizeLimitDataFrameMarshaller();
    }

    /**
     * Check that if we can't fully read frame return position to original state
     */
    @Test
    void decodeNoFullyReceivedDataFrame() {
        // emulate buffer that retrieve only header without data
        sizeLimitFrameDecoder.writeHeader(buffer, 4);
        buffer.flip();

        ByteBuffer frm = sizeLimitFrameDecoder.decode(buffer);

        assertEquals(0, buffer.position());

        assertNull(frm);
    }

    /**
     * Check that if we can't fully read frame return position to original state
     */
    @Test
    void decodeOneDataFrameInBuffer() {

        // emulate that buffer contain one frame

        sizeLimitFrameDecoder.writeHeader(buffer, 4);
        buffer.putInt(123);
        buffer.flip();

        ByteBuffer decode = sizeLimitFrameDecoder.decode(buffer);


        assertEquals(8, buffer.position(), "8 bytes must be read (header + data)");
        assertEquals(0, buffer.remaining(), () -> "Expected 4 data bytes");

        assertNotNull(decode);
        assertEquals(123, decode.duplicate().getInt());

        assertNull(sizeLimitFrameDecoder.decode(buffer));
    }

    /**
     * Check that if we can't fully read frame return position to original state
     */
    @Test
    void decodeSeveralDataFrameInBuffer() {

        // emulate that buffer contain one frame

        sizeLimitFrameDecoder.writeHeader(buffer, 4);
        buffer.putInt(100);

        sizeLimitFrameDecoder.writeHeader(buffer, 8);
        buffer.putLong(200L);

        sizeLimitFrameDecoder.writeHeader(buffer, 8);
        buffer.putInt(300);
        buffer.putInt(301);


        buffer.flip();


        ByteBuffer frm = sizeLimitFrameDecoder.decode(buffer);

        assertNotNull(frm);

        assertEquals(100, frm.getInt());

        frm = sizeLimitFrameDecoder.decode(buffer);

        assertNotNull(frm);
        assertEquals(200L, frm.getLong());


        frm = sizeLimitFrameDecoder.decode(buffer);

        assertNotNull(frm);
        assertEquals(300, frm.getInt());
        assertEquals(301, frm.getInt());

        assertEquals(32, buffer.position(), "32 bytes must be read (data 20 + 12 header)");
        assertEquals(0, buffer.remaining(), () -> "Expected 0 data bytes");

        frm = sizeLimitFrameDecoder.decode(buffer);

        assertNull(frm);
    }


    /**
     * Check that if we can't fully read frame return position to original state
     */
    @Test
    void decodeSeveralAndPartialDataFrameInBuffer() {

        // emulate that buffer contain one frame

        sizeLimitFrameDecoder.writeHeader(buffer, 4);
        buffer.putInt(100);

        sizeLimitFrameDecoder.writeHeader(buffer, 8);
        buffer.putLong(200L);

        sizeLimitFrameDecoder.writeHeader(buffer, 8);
        buffer.putInt(300);


        buffer.flip();

        ByteBuffer frm = sizeLimitFrameDecoder.decode(buffer);

        assertNotNull(frm);
        assertEquals(100, frm.getInt());


        frm = sizeLimitFrameDecoder.decode(buffer);
        assertNotNull(frm);
        assertEquals(200L, frm.getLong());

        frm = sizeLimitFrameDecoder.decode(buffer);
        assertNull(frm);

        assertEquals(20, buffer.position(), "20 bytes must be read ( 12 bytes data + 8 bytes header)");
        assertEquals(8, buffer.remaining(), () -> "Expected not fully received frame (4 bytes hear + 4 bytes partial data) = 8 bytes");
    }


    /**
     * Check that if we can't fully read frame return position to original state
     */
    @Test
    void bufferWriteReadWriteRead() {

        // emulate that buffer contain one frame

        sizeLimitFrameDecoder.writeHeader(buffer, 4);
        buffer.putInt(100);

        sizeLimitFrameDecoder.writeHeader(buffer, 8);
        buffer.putLong(200L);

        buffer.flip();


        ByteBuffer frm = sizeLimitFrameDecoder.decode(buffer);

        assertNotNull(frm);
        assertEquals(100, frm.getInt());

        buffer.compact();
//        buffer.flip();

        sizeLimitFrameDecoder.writeHeader(buffer, 8);
        buffer.putInt(300);
        buffer.putInt(301);

        buffer.flip();

        frm = sizeLimitFrameDecoder.decode(buffer);

        assertNotNull(frm);
        assertEquals(200L, frm.getLong());

        frm = sizeLimitFrameDecoder.decode(buffer);

        assertNotNull(frm);
        assertEquals(300, frm.getInt());
        assertEquals(301, frm.getInt());

        assertEquals(24, buffer.position(), "32 bytes must be read (data 20 + 12 header)");
        assertEquals(0, buffer.remaining(), () -> "Expected 0 data bytes");

        frm = sizeLimitFrameDecoder.decode(buffer);

        assertNull(frm);
    }

    @Test
    void encode() {
    }
}