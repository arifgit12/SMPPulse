package com.smppulse.metrics;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class LatencyHistogram {

    // Buckets: 0-1ms, 1-2ms, 2-5ms, 5-10ms, 10-25ms, 25-50ms, 50-100ms,
    //          100-250ms, 250-500ms, 500-1000ms, 1000-2500ms, 2500-5000ms, 5000+ms
    private static final long[] BUCKET_BOUNDARIES = {
            1, 2, 5, 10, 25, 50, 100, 250, 500, 1000, 2500, 5000
    };
    private static final int BUCKET_COUNT = BUCKET_BOUNDARIES.length + 1;

    private final LongAdder[] buckets;
    private final LongAdder totalCount = new LongAdder();
    private final LongAdder totalSum = new LongAdder();
    private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong max = new AtomicLong(0);

    public LatencyHistogram() {
        buckets = new LongAdder[BUCKET_COUNT];
        for (int i = 0; i < BUCKET_COUNT; i++) {
            buckets[i] = new LongAdder();
        }
    }

    public void record(long latencyMs) {
        int bucketIndex = findBucket(latencyMs);
        buckets[bucketIndex].increment();
        totalCount.increment();
        totalSum.add(latencyMs);
        updateMin(latencyMs);
        updateMax(latencyMs);
    }

    private int findBucket(long value) {
        for (int i = 0; i < BUCKET_BOUNDARIES.length; i++) {
            if (value < BUCKET_BOUNDARIES[i]) {
                return i;
            }
        }
        return BUCKET_COUNT - 1;
    }

    private void updateMin(long value) {
        long current;
        do {
            current = min.get();
            if (value >= current) return;
        } while (!min.compareAndSet(current, value));
    }

    private void updateMax(long value) {
        long current;
        do {
            current = max.get();
            if (value <= current) return;
        } while (!max.compareAndSet(current, value));
    }

    public long getPercentile(double percentile) {
        long total = totalCount.sum();
        if (total == 0) return 0;

        long threshold = (long) Math.ceil(total * percentile / 100.0);
        long cumulative = 0;

        for (int i = 0; i < BUCKET_COUNT; i++) {
            cumulative += buckets[i].sum();
            if (cumulative >= threshold) {
                return i < BUCKET_BOUNDARIES.length ? BUCKET_BOUNDARIES[i] : 10000;
            }
        }
        return 0;
    }

    public long getP50() { return getPercentile(50); }
    public long getP95() { return getPercentile(95); }
    public long getP99() { return getPercentile(99); }

    public double getAverage() {
        long count = totalCount.sum();
        return count > 0 ? (double) totalSum.sum() / count : 0;
    }

    public long getMin() {
        long val = min.get();
        return val == Long.MAX_VALUE ? 0 : val;
    }

    public long getMax() {
        return max.get();
    }

    public long getCount() {
        return totalCount.sum();
    }

    public void reset() {
        for (LongAdder bucket : buckets) {
            bucket.reset();
        }
        totalCount.reset();
        totalSum.reset();
        min.set(Long.MAX_VALUE);
        max.set(0);
    }
}
