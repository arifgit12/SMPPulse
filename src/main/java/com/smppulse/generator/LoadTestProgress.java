package com.smppulse.generator;

public record LoadTestProgress(
    LoadTestState state,
    long messagesSent,
    long totalMessages,
    long elapsedSeconds,
    long totalSeconds,
    double currentTps
) {
    public double progressPercent() {
        if (totalMessages > 0) {
            return (double) messagesSent / totalMessages * 100.0;
        }
        if (totalSeconds > 0) {
            return (double) elapsedSeconds / totalSeconds * 100.0;
        }
        return 0;
    }

    public boolean isComplete() {
        if (totalMessages > 0) {
            return messagesSent >= totalMessages;
        }
        if (totalSeconds > 0) {
            return elapsedSeconds >= totalSeconds;
        }
        return false;
    }
}
