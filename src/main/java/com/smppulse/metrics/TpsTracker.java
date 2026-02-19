package com.smppulse.metrics;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class TpsTracker {

    private static final int WINDOW_SIZE_SECONDS = 300; // 5-minute ring buffer
    private static final int SLOTS_PER_SECOND = 1;

    private final LongAdder[] ringBuffer;
    private final AtomicLong currentSlot = new AtomicLong(0);
    private final AtomicLong peakTps = new AtomicLong(0);
    private final LongAdder totalEvents = new LongAdder();
    private volatile long startTimeMs = 0;

    public TpsTracker() {
        ringBuffer = new LongAdder[WINDOW_SIZE_SECONDS * SLOTS_PER_SECOND];
        for (int i = 0; i < ringBuffer.length; i++) {
            ringBuffer[i] = new LongAdder();
        }
    }

    public void recordEvent() {
        long now = System.currentTimeMillis();
        if (startTimeMs == 0) {
            startTimeMs = now;
        }

        long slot = (now / 1000) % ringBuffer.length;
        long prevSlot = currentSlot.getAndSet(slot);

        // Clear stale slots if we've moved to a new second
        if (slot != prevSlot) {
            ringBuffer[(int) slot].reset();
        }

        ringBuffer[(int) slot].increment();
        totalEvents.increment();

        // Update peak
        long currentTps = ringBuffer[(int) slot].sum();
        long peak;
        do {
            peak = peakTps.get();
            if (currentTps <= peak) break;
        } while (!peakTps.compareAndSet(peak, currentTps));
    }

    public double getCurrentTps() {
        long now = System.currentTimeMillis();
        long slot = (now / 1000) % ringBuffer.length;
        return ringBuffer[(int) slot].sum();
    }

    public double getAverageTps() {
        if (startTimeMs == 0) return 0;
        long elapsedSeconds = (System.currentTimeMillis() - startTimeMs) / 1000;
        if (elapsedSeconds <= 0) return 0;
        return (double) totalEvents.sum() / elapsedSeconds;
    }

    public double getPeakTps() {
        return peakTps.get();
    }

    public double[] getRecentTps(int seconds) {
        long now = System.currentTimeMillis();
        double[] result = new double[Math.min(seconds, ringBuffer.length)];
        for (int i = 0; i < result.length; i++) {
            long slot = ((now / 1000) - (result.length - 1 - i)) % ringBuffer.length;
            if (slot < 0) slot += ringBuffer.length;
            result[i] = ringBuffer[(int) slot].sum();
        }
        return result;
    }

    public void reset() {
        for (LongAdder adder : ringBuffer) {
            adder.reset();
        }
        currentSlot.set(0);
        peakTps.set(0);
        totalEvents.reset();
        startTimeMs = 0;
    }
}
