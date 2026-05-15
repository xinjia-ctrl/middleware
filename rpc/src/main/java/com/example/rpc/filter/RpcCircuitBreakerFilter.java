package com.example.rpc.filter;
import com.example.rpc.protocol.RpcRequest;
import com.example.rpc.protocol.RpcResponse;

import com.example.circuitbreaker.strategy.CircuitBreakerStateMachine;
import com.example.circuitbreaker.strategy.SlidingWindowCircuitBreaker;

import java.util.concurrent.TimeUnit;

public class RpcCircuitBreakerFilter implements RpcFilter {

    private static final int DEFAULT_FAILURE_THRESHOLD = 5;
    private static final int DEFAULT_SUCCESS_THRESHOLD = 3;
    private static final long DEFAULT_TIMEOUT = 10;
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;

    private static final int DEFAULT_MIN_REQUEST = 10;
    private static final double DEFAULT_ERROR_RATIO = 0.5;

    private final CircuitBreakerStateMachine stateMachine;
    private final SlidingWindowCircuitBreaker slidingWindow;
    private final boolean useSlidingWindow;
    private final int successThreshold;
    private final long timeout;
    private final TimeUnit timeUnit;
    private final int minRequest;
    private final double errorRatio;

    // 计数器模式
    public RpcCircuitBreakerFilter() {
        this(new CircuitBreakerStateMachine(), DEFAULT_FAILURE_THRESHOLD, DEFAULT_SUCCESS_THRESHOLD, DEFAULT_TIMEOUT, DEFAULT_TIME_UNIT);
    }

    public RpcCircuitBreakerFilter(CircuitBreakerStateMachine stateMachine, int failureThreshold, int successThreshold, long timeout, TimeUnit timeUnit) {
        this.stateMachine = stateMachine;
        this.slidingWindow = null;
        this.useSlidingWindow = false;
        this.successThreshold = successThreshold;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
        this.minRequest = 0;
        this.errorRatio = 0;
    }

    // 滑动窗口模式
    public RpcCircuitBreakerFilter(SlidingWindowCircuitBreaker slidingWindow, int minRequest, double errorRatio, int successThreshold, long timeout, TimeUnit timeUnit) {
        this.stateMachine = null;
        this.slidingWindow = slidingWindow;
        this.useSlidingWindow = true;
        this.minRequest = minRequest;
        this.errorRatio = errorRatio;
        this.successThreshold = successThreshold;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }

    @Override
    public boolean before(RpcRequest request, RpcResponse response) {
        String key = request.getInterfaceName() + ":" + request.getMethodName();
        boolean acquired;
        if (useSlidingWindow) {
            acquired = slidingWindow.tryAcquire(key, minRequest, errorRatio, successThreshold, timeout, timeUnit);
        } else {
            acquired = stateMachine.tryAcquire(key, DEFAULT_FAILURE_THRESHOLD, successThreshold, timeout, timeUnit);
        }
        if (!acquired) {
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
            if (useSlidingWindow) {
                slidingWindow.recordSuccess(key);
            } else {
                stateMachine.recordSuccess(key);
            }
        } else {
            if (useSlidingWindow) {
                slidingWindow.recordFailure(key);
            } else {
                stateMachine.recordFailure(key);
            }
        }
    }
}
