package com.example.idempotent.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryIdempotentStorageTest {

    private InMemoryIdempotentStorage storage;

    @BeforeEach
    void setUp() {
        storage = new InMemoryIdempotentStorage();
    }

    @Test
    void shouldSaveNewKey() {
        assertTrue(storage.trySave("key-1", 1, TimeUnit.HOURS));
    }

    @Test
    void shouldRejectDuplicateKey() {
        assertTrue(storage.trySave("key-1", 1, TimeUnit.HOURS));
        assertFalse(storage.trySave("key-1", 1, TimeUnit.HOURS));
    }

    @Test
    void differentKeysShouldNotAffectEachOther() {
        assertTrue(storage.trySave("key-a", 1, TimeUnit.HOURS));
        assertTrue(storage.trySave("key-b", 1, TimeUnit.HOURS));
        assertFalse(storage.trySave("key-a", 1, TimeUnit.HOURS));
        assertFalse(storage.trySave("key-b", 1, TimeUnit.HOURS));
    }

    @Test
    void shouldAllowAfterRemove() {
        assertTrue(storage.trySave("key-1", 1, TimeUnit.HOURS));
        storage.remove("key-1");
        assertTrue(storage.trySave("key-1", 1, TimeUnit.HOURS));
    }

    @Test
    void expiredKeyShouldAllowResave() throws InterruptedException {
        assertTrue(storage.trySave("key-1", 10, TimeUnit.MILLISECONDS));
        assertFalse(storage.trySave("key-1", 10, TimeUnit.MILLISECONDS));
        Thread.sleep(50);
        assertTrue(storage.trySave("key-1", 10, TimeUnit.MILLISECONDS));
    }

    @Test
    void evictExpiredShouldRemoveExpiredKeys() throws InterruptedException {
        assertTrue(storage.trySave("key-1", 10, TimeUnit.MILLISECONDS));
        assertTrue(storage.trySave("key-2", 1, TimeUnit.HOURS));
        Thread.sleep(50);
        storage.evictExpired();
        assertTrue(storage.trySave("key-1", 1, TimeUnit.HOURS));
        assertFalse(storage.trySave("key-2", 1, TimeUnit.HOURS));
    }

    @Test
    void shouldCacheAndReturnResult() {
        storage.trySave("key-1", 1, TimeUnit.HOURS);
        storage.saveResult("key-1", "下单成功", 1, TimeUnit.HOURS);
        assertEquals("下单成功", storage.getResult("key-1"));
    }

    @Test
    void removeShouldCleanCache() {
        storage.trySave("key-1", 1, TimeUnit.HOURS);
        storage.saveResult("key-1", "result", 1, TimeUnit.HOURS);
        storage.remove("key-1");
        assertNull(storage.getResult("key-1"));
    }

    @Test
    void getResultShouldReturnNullForMissingKey() {
        assertNull(storage.getResult("nonexistent"));
    }
}
