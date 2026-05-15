package com.example.rpc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public class ConsistentHashLoadBalance implements LoadBalance {

    private final int virtualNodes;
    private final AtomicReference<List<String>> lastAddresses = new AtomicReference<>();
    private final ConcurrentMap<Integer, SortedMap<Long, String>> ringCache = new ConcurrentHashMap<>();

    public ConsistentHashLoadBalance() {
        this(160);
    }

    public ConsistentHashLoadBalance(int virtualNodes) {
        this.virtualNodes = virtualNodes;
    }

    @Override
    public String select(List<String> addresses) {
        throw new RuntimeException("consistent hash need a key for routing");
    }

    @Override
    public String select(List<String> addresses, String key) {
        if (addresses == null || addresses.isEmpty()) {
            throw new RuntimeException("no available address found");
        }
        SortedMap<Long, String> ring = getRing(addresses);
        long hash = hash(key);
        SortedMap<Long, String> tail = ring.tailMap(hash);
        Long nodeHash = tail.isEmpty() ? ring.firstKey() : tail.firstKey();
        return ring.get(nodeHash);
    }

    private SortedMap<Long, String> getRing(List<String> addresses) {
        int hash = addresses.hashCode();
        if (!ringCache.containsKey(hash)) {
            TreeMap<Long, String> ring = new TreeMap<>();
            for (String address : addresses) {
                for (int i = 0; i < virtualNodes; i++) {
                    ring.put(hash(address + "#" + i), address);
                }
            }
            ringCache.put(hash, ring);
        }
        return ringCache.get(hash);
    }

    private long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));
            return ((long) (digest[3] & 0xFF) << 24) | ((long) (digest[2] & 0xFF) << 16)
                    | ((long) (digest[1] & 0xFF) << 8) | (digest[0] & 0xFF);
        } catch (NoSuchAlgorithmException e) {
            long h = 0;
            for (byte b : key.getBytes(StandardCharsets.UTF_8)) {
                h = h * 31 + (b & 0xFF);
            }
            return h;
        }
    }
}
