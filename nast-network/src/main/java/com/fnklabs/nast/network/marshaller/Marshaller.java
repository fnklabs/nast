package com.fnklabs.nast.network.marshaller;

public interface Marshaller<SerializedType, DeserializedType> {

    SerializedType serialize(DeserializedType input) throws MarshallingException;

    DeserializedType deserialize(SerializedType input) throws MarshallingException;
}
