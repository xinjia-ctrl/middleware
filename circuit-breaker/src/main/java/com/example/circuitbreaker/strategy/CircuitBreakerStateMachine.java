package com.example.circuitbreaker.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CircuitBreakerStateMachine {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerStateMachine.class);

    private final Map<String, CircuitBreakerTask> taskMap = new ConcurrentHashMap<>();

    public boolean tryAcquire(String key, int failureThreshold, int successThreshold, long timeout, TimeUnit timeUnit) {
        CircuitBreakerTask task = taskMap.computeIfAbsent(key,
                k -> new CircuitBreakerTask(failureThreshold, successThreshold, timeout, timeUnit));
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
                        if (tryTransition(CircuitBreakerState.OPEN, CircuitBreakerState.HALF_OPEN, now)) {
                            log.debug("circuit half-open: allowing probe request");
                            return true;
                        }
                    }
                    return false;
                case HALF_OPEN:
                    // timeout escape from HALF_OPEN — prevents permanent stuck if probes never complete
                    if (now - lastStateChangeTime >= timeoutNanos) {
                        if (tryTransition(CircuitBreakerState.HALF_OPEN, CircuitBreakerState.OPEN, now)) {
                            log.debug("circuit half-open timeout, back to open");
                        }
                        return false;
                    }
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
                        transitionTo(CircuitBreakerState.CLOSED, System.nanoTime());
                        log.info("circuit closed after half-open success threshold reached");
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
                        transitionTo(CircuitBreakerState.OPEN, now);
                        log.warn("circuit opened after {} failures", failureThreshold);
                    }
                    break;
                case HALF_OPEN:
                    transitionTo(CircuitBreakerState.OPEN, now);
                    log.warn("circuit re-opened after half-open probe failure");
                    break;
                case OPEN:
                    break;
            }
        }

        private synchronized boolean tryTransition(CircuitBreakerState from, CircuitBreakerState to, long now) {
            if (this.state == from) {
                doTransition(to, now);
                return true;
            }
            return false;
        }

        private synchronized void transitionTo(CircuitBreakerState newState, long now) {
            doTransition(newState, now);
        }

        private void doTransition(CircuitBreakerState newState, long now) {
            this.state = newState;
            this.lastStateChangeTime = now;
            this.failureCount.set(0);
            this.successCount.set(0);
            this.halfOpenPermits.set(0);
            if (newState == CircuitBreakerState.HALF_OPEN) {
                halfOpenPermits.set(successThreshold);
            }
        }

        CircuitBreakerState getState() {
            return state;
        }
    }
}
