package com.fnklabs.nast.network.marshaller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class JavaObjectMarshaller implements Marshaller<byte[], Object> {

    @Override
    public byte[] serialize(Object input) throws MarshallingException {
        try (ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
             ObjectOutputStream os = new ObjectOutputStream(bytearrayoutputstream)) {

            os.writeObject(input);

            return bytearrayoutputstream.toByteArray();

        } catch (IOException e) {
            throw new MarshallingException("can't serialize data", e);
        }
    }

    @Override
    public Object deserialize(byte[] input) throws MarshallingException {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(input);
             ObjectInputStream is = new ObjectInputStream(byteArrayInputStream)) {

            return is.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new MarshallingException("can't serialize data", e);
        }

    }
}
