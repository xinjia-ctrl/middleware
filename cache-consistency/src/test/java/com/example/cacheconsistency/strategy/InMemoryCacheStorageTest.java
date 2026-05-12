package com.example.cacheconsistency.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryCacheStorageTest {

    private InMemoryCacheStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryCacheStorage();
    }

    @Test
    void shouldDeleteExistingKey() {
        storage.set("key1", "value1", 10, TimeUnit.SECONDS);
        assertEquals(1, storage.size());

        storage.delete("key1");
        assertEquals(0, storage.size());
    }

    @Test
    void shouldDeleteNonExistentKey() {
        storage.delete("non-existent");
        assertEquals(0, storage.size());
    }

    @Test
    void shouldSetAndGetValue() {
        storage.set("key1", "value1", 10, TimeUnit.SECONDS);

        String value = storage.get("key1");
        assertEquals("value1", value);
    }

    @Test
    void shouldReturnNullForExpiredKey() throws Exception {
        storage.set("expired-key", "value", 1, TimeUnit.MILLISECONDS);
        Thread.sleep(10);

        assertNull(storage.get("expired-key"));
        assertEquals(0, storage.size());
    }

    @Test
    void shouldOverwriteExistingKey() {
        storage.set("key1", "value1", 10, TimeUnit.SECONDS);
        storage.set("key1", "value2", 10, TimeUnit.SECONDS);

        assertEquals("value2", storage.get("key1"));
    }

    @Test
    void shouldReturnNullForUnknownKey() {
        assertNull(storage.get("unknown"));
    }

    @Test
    void shouldHandleMultipleKeys() {
        storage.set("k1", "v1", 10, TimeUnit.SECONDS);
        storage.set("k2", "v2", 10, TimeUnit.SECONDS);
        storage.set("k3", "v3", 10, TimeUnit.SECONDS);

        assertEquals(3, storage.size());

        storage.delete("k2");
        assertEquals(2, storage.size());
        assertNull(storage.get("k2"));
        assertNotNull(storage.get("k1"));
        assertNotNull(storage.get("k3"));
    }

    @Test
    void shouldEvictExpiredOnSizeCheck() throws Exception {
        storage.set("valid", "v", 10, TimeUnit.SECONDS);
        storage.set("expired", "v", 1, TimeUnit.MILLISECONDS);
        Thread.sleep(10);

        assertEquals(1, storage.size());
        assertNull(storage.get("expired"));
        assertNotNull(storage.get("valid"));
    }
}
