package com.example.rpc;

import com.example.ratelimit.strategy.RateLimitStrategy;
import com.example.ratelimit.strategy.TokenBucketStrategy;

public class RpcRateLimitFilter implements RpcFilter {

    private final RateLimitStrategy strategy;

    public RpcRateLimitFilter(RateLimitStrategy strategy) {
        this.strategy = strategy;
    }

    public RpcRateLimitFilter() {
        this(new TokenBucketStrategy(100, 100));
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
