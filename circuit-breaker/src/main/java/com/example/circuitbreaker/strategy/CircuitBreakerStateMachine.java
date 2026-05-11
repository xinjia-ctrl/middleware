package com.example.circuitbreaker.strategy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CircuitBreakerStateMachine {

    private final Map<String, CircuitBreakerTask> taskMap = new ConcurrentHashMap<>();

    public boolean tryAcquire(String key, int failureThreshold, int successThreshold, long timeout, TimeUnit timeUnit) {
        CircuitBreakerTask task = taskMap.computeIfAbsent(key, k -> new CircuitBreakerTask(failureThreshold, successThreshold, timeout, timeUnit));
        return task.tryAcquire();
    }

    public void recordSuccess(String key) {
        CircuitBreakerTask task = taskMap.get(key);
        if (task != null) {
            task.recordSuccess();
        }
    }

    public void recordFailure(String key) {
        CircuitBreakerTask task = taskMap.get(key);
        if (task != null) {
            task.recordFailure();
        }
    }

    public CircuitBreakerState getState(String key) {
        CircuitBreakerTask task = taskMap.get(key);
        if (task == null) {
            return CircuitBreakerState.CLOSED;
        }
        return task.getState();
    }

    public void reset(String key) {
        taskMap.remove(key);
    }

    static class CircuitBreakerTask {
        private volatile CircuitBreakerState state = CircuitBreakerState.CLOSED;
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger halfOpenPermits = new AtomicInteger(0);
        private final int failureThreshold;
        private final int successThreshold;
        private final long timeoutNanos;
        private volatile long lastStateChangeTime;

        CircuitBreakerTask(int failureThreshold, int successThreshold, long timeout, TimeUnit timeUnit) {
            this.failureThreshold = failureThreshold;
            this.successThreshold = successThreshold;
            this.timeoutNanos = timeUnit.toNanos(timeout);
            this.lastStateChangeTime = System.nanoTime();
        }

        boolean tryAcquire() {
            long now = System.nanoTime();
            CircuitBreakerState currentState = this.state;

            switch (currentState) {
                case CLOSED:
                    return true;
                case OPEN:
                    if (now - lastStateChangeTime >= timeoutNanos) {
                        if (tryTransition(OPEN, HALF_OPEN, now)) {
                            return true;
                        }
                    }
                    return false;
                case HALF_OPEN:
                    return halfOpenPermits.decrementAndGet() >= 0;
                default:
                    return false;
            }
        }

        synchronized void recordSuccess() {
            CircuitBreakerState currentState = this.state;

            switch (currentState) {
                case CLOSED:
                    failureCount.set(0);
                    break;
                case HALF_OPEN:
                    if (successCount.incrementAndGet() >= successThreshold) {
                        resetToClosed();
                    }
                    break;
                default:
                    break;
            }
        }

        synchronized void recordFailure() {
            CircuitBreakerState currentState = this.state;
            long now = System.nanoTime();

            switch (currentState) {
                case CLOSED:
                    if (failureCount.incrementAndGet() >= failureThreshold) {
                        transitionTo(OPEN, now);
                    }
                    break;
                case HALF_OPEN:
                    transitionTo(OPEN, now);
                    break;
                case OPEN:
                    break;
            }
        }

        private boolean tryTransition(CircuitBreakerState from, CircuitBreakerState to, long now) {
            synchronized (this) {
                if (this.state == from) {
                    this.state = to;
                    this.lastStateChangeTime = now;
                    this.successCount.set(0);
                    if (to == CircuitBreakerState.HALF_OPEN) {
                        halfOpenPermits.set(successThreshold);
                    } else if (to == CircuitBreakerState.CLOSED) {
                        this.failureCount.set(0);
                    }
                    return true;
                }
                return false;
            }
        }

        private void transitionTo(CircuitBreakerState newState, long now) {
            this.state = newState;
            this.lastStateChangeTime = now;
            if (newState == CircuitBreakerState.OPEN) {
                this.successCount.set(0);
                this.halfOpenPermits.set(0);
            } else if (newState == CircuitBreakerState.CLOSED) {
                this.failureCount.set(0);
                this.successCount.set(0);
                this.halfOpenPermits.set(0);
            }
        }

        private void resetToClosed() {
            this.state = CircuitBreakerState.CLOSED;
            this.lastStateChangeTime = System.nanoTime();
            this.failureCount.set(0);
            this.successCount.set(0);
            this.halfOpenPermits.set(0);
        }

        CircuitBreakerState getState() {
            return state;
        }
    }
}
