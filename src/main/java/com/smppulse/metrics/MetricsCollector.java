package com.smppulse.metrics;

import java.time.Instant;
import java.util.concurrent.atomic.LongAdder;

public class MetricsCollector {

    private final LongAdder submitted = new LongAdder();
    private final LongAdder success = new LongAdder();
    private final LongAdder failed = new LongAdder();
    private final LongAdder throttled = new LongAdder();
    private final LongAdder timeouts = new LongAdder();
    private final LongAdder dlrReceived = new LongAdder();
    private final LongAdder dlrPending = new LongAdder();
    private final LongAdder dlrTimedOut = new LongAdder();
    private final LongAdder dlrSuccess = new LongAdder();
    private final LongAdder dlrFailed = new LongAdder();

    private final LatencyHistogram latencyHistogram = new LatencyHistogram();
    private final TpsTracker tpsTracker = new TpsTracker();

    private volatile Instant startTime;
    private volatile Instant endTime;

    public void recordSubmitted() {
        submitted.increment();
        tpsTracker.recordEvent();
    }

    public void recordSuccess(long latencyMs) {
        success.increment();
        latencyHistogram.record(latencyMs);
    }

    public void recordFailure() {
        failed.increment();
    }

    public void recordThrottle() {
        throttled.increment();
    }

    public void recordTimeout() {
        timeouts.increment();
    }

    public void recordDlrReceived() { dlrReceived.increment(); }
    public void recordDlrPending() { dlrPending.increment(); }
    public void decrementDlrPending() { dlrPending.decrement(); }
    public void recordDlrTimedOut() { dlrTimedOut.increment(); }
    public void recordDlrSuccess() { dlrSuccess.increment(); }
    public void recordDlrFailed() { dlrFailed.increment(); }

    public void markStart() {
        startTime = Instant.now();
        endTime = null;
    }

    public void markEnd() {
        endTime = Instant.now();
    }

    public MetricsSnapshot snapshot() {
        return new MetricsSnapshot(
                submitted.sum(),
                success.sum(),
                failed.sum(),
                throttled.sum(),
                timeouts.sum(),
                dlrReceived.sum(),
                dlrPending.sum(),
                dlrTimedOut.sum(),
                dlrSuccess.sum(),
                dlrFailed.sum(),
                latencyHistogram.getP50(),
                latencyHistogram.getP95(),
                latencyHistogram.getP99(),
                latencyHistogram.getAverage(),
                latencyHistogram.getMin(),
                latencyHistogram.getMax(),
                tpsTracker.getCurrentTps(),
                tpsTracker.getAverageTps(),
                tpsTracker.getPeakTps(),
                startTime,
                endTime
        );
    }

    public void reset() {
        submitted.reset();
        success.reset();
        failed.reset();
        throttled.reset();
        timeouts.reset();
        dlrReceived.reset();
        dlrPending.reset();
        dlrTimedOut.reset();
        dlrSuccess.reset();
        dlrFailed.reset();
        latencyHistogram.reset();
        tpsTracker.reset();
        startTime = null;
        endTime = null;
    }

    public TpsTracker getTpsTracker() { return tpsTracker; }
    public LatencyHistogram getLatencyHistogram() { return latencyHistogram; }
}
