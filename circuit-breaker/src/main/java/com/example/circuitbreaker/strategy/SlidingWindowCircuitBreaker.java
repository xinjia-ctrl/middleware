package com.example.circuitbreaker.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 滑动窗口熔断器
 * 10s 滑动窗口记录请求成功/失败，基于失败比例判定熔断
 * 样本不足（< minRequest）时不触发熔断
 */
public class SlidingWindowCircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(SlidingWindowCircuitBreaker.class);
    private static final long WINDOW_MILLIS = 10_000L;

    private final Map<String, WindowTask> taskMap = new ConcurrentHashMap<>();

    public boolean tryAcquire(String key, int minRequest, double errorRatio,
                               int successThreshold, long timeout, TimeUnit timeUnit) {
        WindowTask task = taskMap.computeIfAbsent(key,
                k -> new WindowTask(minRequest, errorRatio, successThreshold, timeUnit.toNanos(timeout)));
        return task.tryAcquire();
    }

    public void recordSuccess(String key) {
        WindowTask task = taskMap.get(key);
        if (task != null) task.recordSuccess();
    }

    public void recordFailure(String key) {
        WindowTask task = taskMap.get(key);
        if (task != null) task.recordFailure();
    }

    public CircuitBreakerState getState(String key) {
        WindowTask task = taskMap.get(key);
        return task == null ? CircuitBreakerState.CLOSED : task.getState();
    }

    public void reset(String key) {
        taskMap.remove(key);
    }

    static class RequestRecord {
        final long timestamp;
        final boolean failure;

        RequestRecord(long timestamp, boolean failure) {
            this.timestamp = timestamp;
            this.failure = failure;
        }
    }

    static class SlidingWindow {
        private final Deque<RequestRecord> records = new ArrayDeque<>();
        private final ReentrantLock lock = new ReentrantLock();
        private int failureCount;

        void add(boolean failure) {
            long now = System.currentTimeMillis();
            lock.lock();
            try {
                trim(now);
                records.addLast(new RequestRecord(now, failure));
                if (failure) {
                    failureCount++;
                }
            } finally {
                lock.unlock();
            }
        }

        /** atomically returns total and failure count */
        int[] snapshot() {
            long now = System.currentTimeMillis();
            lock.lock();
            try {
                trim(now);
                return new int[]{records.size(), failureCount};
            } finally {
                lock.unlock();
            }
        }

        private void trim(long now) {
            long cutoff = now - WINDOW_MILLIS;
            while (!records.isEmpty() && records.peekFirst().timestamp < cutoff) {
                if (records.removeFirst().failure) {
                    failureCount--;
                }
            }
        }
    }

    static class WindowTask {
        private volatile CircuitBreakerState state = CircuitBreakerState.CLOSED;
        private final int minRequest;
        private final double errorRatio;
        private final int successThreshold;
        private final long timeoutNanos;
        private volatile long lastStateChangeTime;
        private final SlidingWindow window = new SlidingWindow();
        private final AtomicInteger consecutiveSuccess = new AtomicInteger(0);
        private final AtomicInteger halfOpenPermits = new AtomicInteger(0);

        WindowTask(int minRequest, double errorRatio, int successThreshold, long timeoutNanos) {
            this.minRequest = minRequest;
            this.errorRatio = errorRatio;
            this.successThreshold = successThreshold;
            this.timeoutNanos = timeoutNanos;
            this.lastStateChangeTime = System.nanoTime();
        }

        boolean tryAcquire() {
            long now = System.nanoTime();
            switch (state) {
                case CLOSED:
                    return true;
                case OPEN:
                    if (now - lastStateChangeTime >= timeoutNanos) {
                        if (tryTransition(CircuitBreakerState.OPEN, CircuitBreakerState.HALF_OPEN, now)) {
                            log.debug("sliding-window circuit half-open: allowing probe");
                            return true;
                        }
                    }
                    return false;
                case HALF_OPEN:
                    // timeout escape from HALF_OPEN
                    if (now - lastStateChangeTime >= timeoutNanos) {
                        if (tryTransition(CircuitBreakerState.HALF_OPEN, CircuitBreakerState.OPEN, now)) {
                            log.debug("sliding-window half-open timeout, back to open");
                        }
                        return false;
                    }
                    return halfOpenPermits.decrementAndGet() >= 0;
                default:
                    return false;
            }
        }

        void recordSuccess() {
            if (state == CircuitBreakerState.CLOSED || state == CircuitBreakerState.HALF_OPEN) {
                window.add(false);
            }
            synchronized (this) {
                if (state == CircuitBreakerState.HALF_OPEN) {
                    if (consecutiveSuccess.incrementAndGet() >= successThreshold) {
                        transitionTo(CircuitBreakerState.CLOSED, System.nanoTime());
                        log.info("sliding-window circuit closed after half-open success");
                    }
                }
            }
        }

        void recordFailure() {
            if (state == CircuitBreakerState.CLOSED || state == CircuitBreakerState.HALF_OPEN) {
                window.add(true);
            }
            synchronized (this) {
                if (state == CircuitBreakerState.HALF_OPEN) {
                    transitionTo(CircuitBreakerState.OPEN, System.nanoTime());
                    log.warn("sliding-window circuit re-opened after half-open failure");
                } else if (state == CircuitBreakerState.CLOSED) {
                    int[] stats = window.snapshot();
                    int total = stats[0];
                    int failed = stats[1];
                    if (total >= minRequest && (double) failed / total >= errorRatio) {
                        transitionTo(CircuitBreakerState.OPEN, System.nanoTime());
                        log.warn("sliding-window circuit opened: {}/{} failed", failed, total);
                    }
                }
            }
        }

        private boolean tryTransition(CircuitBreakerState from, CircuitBreakerState to, long now) {
            synchronized (this) {
                if (this.state == from) {
                    doTransition(to, now);
                    return true;
                }
                return false;
            }
        }

        private synchronized void transitionTo(CircuitBreakerState newState, long now) {
            doTransition(newState, now);
        }

        private void doTransition(CircuitBreakerState newState, long now) {
            this.state = newState;
            this.lastStateChangeTime = now;
            this.consecutiveSuccess.set(0);
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
