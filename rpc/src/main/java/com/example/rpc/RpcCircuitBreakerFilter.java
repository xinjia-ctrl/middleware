package com.example.rpc;

import com.example.circuitbreaker.strategy.CircuitBreakerStateMachine;

import java.util.concurrent.TimeUnit;

public class RpcCircuitBreakerFilter implements RpcFilter {

    private static final int DEFAULT_FAILURE_THRESHOLD = 5;
    private static final int DEFAULT_SUCCESS_THRESHOLD = 3;
    private static final long DEFAULT_TIMEOUT = 10;
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;

    private final CircuitBreakerStateMachine stateMachine;
    private final int failureThreshold;
    private final int successThreshold;
    private final long timeout;
    private final TimeUnit timeUnit;

    public RpcCircuitBreakerFilter() {
        this(new CircuitBreakerStateMachine(), DEFAULT_FAILURE_THRESHOLD, DEFAULT_SUCCESS_THRESHOLD, DEFAULT_TIMEOUT, DEFAULT_TIME_UNIT);
    }

    public RpcCircuitBreakerFilter(CircuitBreakerStateMachine stateMachine, int failureThreshold, int successThreshold, long timeout, TimeUnit timeUnit) {
        this.stateMachine = stateMachine;
        this.failureThreshold = failureThreshold;
        this.successThreshold = successThreshold;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }

    @Override
    public boolean before(RpcRequest request, RpcResponse response) {
        String key = request.getInterfaceName() + ":" + request.getMethodName();
        if (!stateMachine.tryAcquire(key, failureThreshold, successThreshold, timeout, timeUnit)) {
            response.setCode(503);
            response.setMessage("circuit breaker open, try again later");
            return false;
        }
        return true;
    }

    @Override
    public void after(RpcRequest request, RpcResponse response) {
        String key = request.getInterfaceName() + ":" + request.getMethodName();
        if (response.getCode() == 200) {
            stateMachine.recordSuccess(key);
        } else {
            stateMachine.recordFailure(key);
        }
    }
}
