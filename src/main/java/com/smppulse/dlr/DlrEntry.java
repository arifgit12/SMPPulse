package com.smppulse.dlr;

import java.time.Instant;

public record DlrEntry(
    String messageId,
    String sourceAddr,
    String destAddr,
    Instant submitTime,
    Instant dlrTime,
    DlrStatus status
) {
    public enum DlrStatus {
        PENDING, DELIVERED, FAILED, EXPIRED, REJECTED, UNKNOWN, TIMED_OUT
    }

    public DlrEntry(String messageId, String sourceAddr, String destAddr) {
        this(messageId, sourceAddr, destAddr, Instant.now(), null, DlrStatus.PENDING);
    }

    public DlrEntry withDlr(DlrStatus status) {
        return new DlrEntry(messageId, sourceAddr, destAddr, submitTime, Instant.now(), status);
    }

    public long latencyMs() {
        if (dlrTime == null) return -1;
        return dlrTime.toEpochMilli() - submitTime.toEpochMilli();
    }
}
