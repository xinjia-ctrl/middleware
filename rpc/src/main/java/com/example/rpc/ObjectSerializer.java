package com.example.rpc;

import java.io.*;

public class ObjectSerializer implements Serializer {

    @Override
    public short getType() {
        return 0;
    }

    @Override
    public byte[] serialize(Object obj) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
        } catch (IOException e) {
            throw new RuntimeException("ObjectSerializer serialize failed", e);
        }
        return bos.toByteArray();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return (T) ois.readObject();
        } catch (Exception e) {
            throw new RuntimeException("ObjectSerializer deserialize failed", e);
        }
    }
}
