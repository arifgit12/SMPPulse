package com.smppulse.core.codec;

import java.time.Instant;

public record PduRecord(
    Instant timestamp,
    String direction,
    String pduType,
    String messageId,
    String sourceAddr,
    String destAddr,
    int commandStatus,
    String details
) {
    public PduRecord(String direction, String pduType, String messageId,
                     String sourceAddr, String destAddr) {
        this(Instant.now(), direction, pduType, messageId, sourceAddr, destAddr, 0, "");
    }

    public PduRecord(String direction, String pduType, String messageId,
                     String sourceAddr, String destAddr, int commandStatus) {
        this(Instant.now(), direction, pduType, messageId, sourceAddr, destAddr, commandStatus, "");
    }
}
