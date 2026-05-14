package com.example.rpc;

public interface Serializer {

    short getType();

    byte[] serialize(Object obj);

    <T> T deserialize(byte[] data, Class<T> clazz);
}
