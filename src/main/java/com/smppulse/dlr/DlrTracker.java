package com.smppulse.dlr;

import com.smppulse.metrics.MetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;

public class DlrTracker {

    private static final Logger log = LoggerFactory.getLogger(DlrTracker.class);

    private final Map<String, DlrEntry> pendingDlrs = new ConcurrentHashMap<>();
    private final MetricsCollector metricsCollector;
    private volatile int timeoutSeconds = 60;
    private final ScheduledExecutorService timeoutChecker;

    public DlrTracker(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
        this.timeoutChecker = Executors.newSingleThreadScheduledExecutor(
                Thread.ofVirtual().name("dlr-timeout-", 0).factory()
        );
        this.timeoutChecker.scheduleAtFixedRate(this::checkTimeouts, 5, 5, TimeUnit.SECONDS);
    }

    public void trackMessage(String messageId, String sourceAddr, String destAddr) {
        DlrEntry entry = new DlrEntry(messageId, sourceAddr, destAddr);
        pendingDlrs.put(messageId, entry);
        metricsCollector.recordDlrPending();
    }

    public void receiveDlr(String messageId, DlrEntry.DlrStatus status) {
        DlrEntry entry = pendingDlrs.remove(messageId);
        if (entry != null) {
            DlrEntry completed = entry.withDlr(status);
            metricsCollector.decrementDlrPending();
            metricsCollector.recordDlrReceived();

            if (status == DlrEntry.DlrStatus.DELIVERED) {
                metricsCollector.recordDlrSuccess();
            } else if (status == DlrEntry.DlrStatus.FAILED ||
                       status == DlrEntry.DlrStatus.REJECTED ||
                       status == DlrEntry.DlrStatus.EXPIRED) {
                metricsCollector.recordDlrFailed();
            }

            log.debug("DLR received for {}: {} (latency: {}ms)", messageId, status, completed.latencyMs());
        } else {
            metricsCollector.recordDlrReceived();
            log.debug("DLR received for unknown message: {}", messageId);
        }
    }

    private void checkTimeouts() {
        Instant cutoff = Instant.now().minus(Duration.ofSeconds(timeoutSeconds));
        pendingDlrs.entrySet().removeIf(entry -> {
            if (entry.getValue().submitTime().isBefore(cutoff)) {
                metricsCollector.decrementDlrPending();
                metricsCollector.recordDlrTimedOut();
                log.debug("DLR timeout for message: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getPendingCount() {
        return pendingDlrs.size();
    }

    public void clear() {
        pendingDlrs.clear();
    }

    public void shutdown() {
        timeoutChecker.shutdownNow();
        pendingDlrs.clear();
    }
}
