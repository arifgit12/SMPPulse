package com.smppulse.dlr;

import com.smppulse.metrics.MetricsCollector;
import org.jsmpp.bean.DeliverSm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DlrHandler {

    private static final Logger log = LoggerFactory.getLogger(DlrHandler.class);

    private final DlrTracker dlrTracker;
    private final MetricsCollector metricsCollector;

    public DlrHandler(DlrTracker dlrTracker, MetricsCollector metricsCollector) {
        this.dlrTracker = dlrTracker;
        this.metricsCollector = metricsCollector;
    }

    public void handleDlr(DeliverSm deliverSm) {
        try {
            String dlrText = new String(deliverSm.getShortMessage());
            String messageId = DlrFormat.extractMessageId(dlrText);
            DlrEntry.DlrStatus status = DlrFormat.extractStatus(dlrText);

            if (messageId != null) {
                dlrTracker.receiveDlr(messageId, status);
            } else {
                log.warn("Could not extract message ID from DLR: {}", dlrText);
                metricsCollector.recordDlrReceived();
            }
        } catch (Exception e) {
            log.warn("Error processing DLR", e);
        }
    }
}
