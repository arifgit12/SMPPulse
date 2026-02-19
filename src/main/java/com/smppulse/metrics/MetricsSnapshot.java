package com.smppulse.metrics;

import java.time.Duration;
import java.time.Instant;

public record MetricsSnapshot(
    long submitted,
    long success,
    long failed,
    long throttled,
    long timeouts,
    long dlrReceived,
    long dlrPending,
    long dlrTimedOut,
    long dlrSuccess,
    long dlrFailed,
    long latencyP50,
    long latencyP95,
    long latencyP99,
    double latencyAvg,
    long latencyMin,
    long latencyMax,
    double currentTps,
    double averageTps,
    double peakTps,
    Instant startTime,
    Instant endTime
) {
    public Duration elapsed() {
        if (startTime == null) return Duration.ZERO;
        Instant end = endTime != null ? endTime : Instant.now();
        return Duration.between(startTime, end);
    }

    public double successRate() {
        long total = success + failed;
        return total > 0 ? (double) success / total * 100.0 : 0;
    }
}
