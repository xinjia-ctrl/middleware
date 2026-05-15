package com.example.rpc.filter;
import com.example.rpc.protocol.RpcRequest;
import com.example.rpc.protocol.RpcResponse;

import com.example.ratelimit.strategy.LockFreeRateLimiter;
import com.example.ratelimit.strategy.RateLimitStrategy;

public class RpcRateLimitFilter implements RpcFilter {

    private final RateLimitStrategy strategy;

    public RpcRateLimitFilter(RateLimitStrategy strategy) {
        this.strategy = strategy;
    }

    public RpcRateLimitFilter() {
        this(new LockFreeRateLimiter(100));
    }

    @Override
    public boolean before(RpcRequest request, RpcResponse response) {
        String key = request.getInterfaceName() + ":" + request.getMethodName();
        if (!strategy.tryAcquire(key, 1)) {
            response.setCode(429);
            response.setMessage("rate limited, try again later");
            return false;
        }
        return true;
    }
}
