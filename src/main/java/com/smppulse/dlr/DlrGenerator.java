package com.smppulse.dlr;

import com.smppulse.config.DlrConfig;
import com.smppulse.metrics.MetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

public class DlrGenerator {

    private static final Logger log = LoggerFactory.getLogger(DlrGenerator.class);
    private static final DateTimeFormatter SMPP_DATE_FORMAT = DateTimeFormatter.ofPattern("yyMMddHHmmss");

    private final MetricsCollector metricsCollector;
    private final ScheduledExecutorService scheduler;

    public DlrGenerator(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
        this.scheduler = Executors.newScheduledThreadPool(2,
                Thread.ofVirtual().name("dlr-gen-", 0).factory());
    }

    public void scheduleDlr(String messageId, String sourceAddr, String destAddr,
                             DlrConfig config, DlrDeliveryCallback callback) {
        if (!config.isGenerateDlr()) return;

        scheduler.schedule(() -> {
            try {
                boolean success = ThreadLocalRandom.current().nextDouble() < config.getDlrSuccessRate();
                String stat = success ? "DELIVRD" : "UNDELIV";
                String now = LocalDateTime.now().format(SMPP_DATE_FORMAT);

                String dlrText = String.format(config.getDlrFormat(), messageId, now, now, stat);

                callback.onDlrReady(messageId, sourceAddr, destAddr, dlrText, success);

                if (success) {
                    metricsCollector.recordDlrSuccess();
                } else {
                    metricsCollector.recordDlrFailed();
                }
            } catch (Exception e) {
                log.warn("Error generating DLR for {}", messageId, e);
            }
        }, config.getDlrDelayMs(), TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    @FunctionalInterface
    public interface DlrDeliveryCallback {
        void onDlrReady(String messageId, String sourceAddr, String destAddr,
                        String dlrText, boolean success);
    }
}
