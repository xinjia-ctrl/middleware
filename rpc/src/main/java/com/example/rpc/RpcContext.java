package com.example.rpc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RpcContext {

    private static final ThreadLocal<RpcContext> LOCAL = ThreadLocal.withInitial(RpcContext::new);

    private final Map<String, Object> attachments = new HashMap<>();

    public static RpcContext get() {
        return LOCAL.get();
    }

    public static void remove() {
        LOCAL.remove();
    }

    public RpcContext setAttachment(String key, Object value) {
        attachments.put(key, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttachment(String key) {
        return (T) attachments.get(key);
    }

    public Map<String, Object> getAttachments() {
        return Collections.unmodifiableMap(attachments);
    }

    public RpcContext clear() {
        attachments.clear();
        return this;
    }
}
