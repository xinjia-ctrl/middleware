package com.example.rpc.filter;
import com.example.rpc.protocol.RpcRequest;
import com.example.rpc.protocol.RpcResponse;

import com.example.idempotent.strategy.IdempotentStorage;
import com.example.idempotent.strategy.InMemoryIdempotentStorage;

import java.util.concurrent.TimeUnit;

public class RpcIdempotentFilter implements RpcFilter {

    private final IdempotentStorage storage;
    private final long ttl;
    private final TimeUnit timeUnit;

    public RpcIdempotentFilter(IdempotentStorage storage, long ttl, TimeUnit timeUnit) {
        this.storage = storage;
        this.ttl = ttl;
        this.timeUnit = timeUnit;
    }

    public RpcIdempotentFilter() {
        this(new InMemoryIdempotentStorage(), 5, TimeUnit.SECONDS);
    }

    @Override
    public boolean before(RpcRequest request, RpcResponse response) {
        String key = request.getInterfaceName() + ":" + request.getRequestId();
        boolean saved = storage.trySave(key, ttl, timeUnit);
        if (!saved) {
            Object cached = storage.getResult(key);
            if (cached != null) {
                response.setCode(200);
                response.setData(cached);
            } else {
                response.setCode(500);
                response.setMessage("duplicate request");
            }
            return false;
        }
        return true;
    }

    @Override
    public void after(RpcRequest request, RpcResponse response) {
        String key = request.getInterfaceName() + ":" + request.getRequestId();
        if (response.getCode() != 200) {
            storage.remove(key);
        } else {
            storage.saveResult(key, response.getData(), ttl, timeUnit);
        }
    }
}
