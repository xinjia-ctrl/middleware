package com.example.rpc.core.serialization;

/**
 * 序列化接口，所有序列化器需实现此接口
 */
public interface Serializer {

    byte[] serialize(Object obj);

    <T> T deserialize(byte[] data, Class<T> clazz);

    /**
     * 序列化类型编码，用于协议头标识
     */
    byte getType();
}
