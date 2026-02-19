package com.smppulse.generator;

import java.util.concurrent.atomic.AtomicLong;

public class RateLimiter {

    private volatile double permitsPerSecond;
    private final AtomicLong nextFreeTicketMicros = new AtomicLong(0);
    private volatile double storedPermits = 0;
    private volatile double maxStoredPermits;
    private volatile double stableIntervalMicros;

    // Ramp configuration
    private volatile double startTps;
    private volatile double endTps;
    private volatile long rampDurationMs;
    private volatile long rampStartTimeMs;

    public RateLimiter(double permitsPerSecond) {
        setRate(permitsPerSecond);
    }

    public synchronized void setRate(double permitsPerSecond) {
        this.permitsPerSecond = Math.max(0.1, permitsPerSecond);
        this.stableIntervalMicros = 1_000_000.0 / this.permitsPerSecond;
        this.maxStoredPermits = this.permitsPerSecond;
        this.storedPermits = Math.min(storedPermits, maxStoredPermits);
    }

    public void configureRamp(double startTps, double endTps, long rampDurationMs) {
        this.startTps = startTps;
        this.endTps = endTps;
        this.rampDurationMs = rampDurationMs;
        this.rampStartTimeMs = System.currentTimeMillis();
        setRate(startTps > 0 ? startTps : 1);
    }

    public boolean tryAcquire() {
        updateRampRate();
        long nowMicros = System.currentTimeMillis() * 1000;
        long next = nextFreeTicketMicros.get();

        if (nowMicros >= next) {
            if (nextFreeTicketMicros.compareAndSet(next, nowMicros + (long) stableIntervalMicros)) {
                return true;
            }
        }
        return false;
    }

    public void acquire() throws InterruptedException {
        while (!tryAcquire()) {
            updateRampRate();
            long waitMicros = nextFreeTicketMicros.get() - System.currentTimeMillis() * 1000;
            if (waitMicros > 0) {
                long waitMs = Math.min(waitMicros / 1000, 10);
                if (waitMs > 0) {
                    Thread.sleep(waitMs);
                } else {
                    Thread.onSpinWait();
                }
            }
        }
    }

    private void updateRampRate() {
        if (rampDurationMs <= 0) return;

        long elapsed = System.currentTimeMillis() - rampStartTimeMs;
        if (elapsed >= rampDurationMs) {
            setRate(endTps);
            rampDurationMs = 0; // Done ramping
            return;
        }

        double progress = (double) elapsed / rampDurationMs;
        double currentRate = startTps + (endTps - startTps) * progress;
        setRate(currentRate);
    }

    public double getCurrentRate() {
        return permitsPerSecond;
    }
}
