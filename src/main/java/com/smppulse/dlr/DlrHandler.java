package com.smppulse.dlr;

import com.smppulse.metrics.MetricsCollector;
import org.jsmpp.bean.DeliverSm;
import org.jsmpp.bean.OptionalParameter;
import org.jsmpp.bean.OptionalParameter.OctetString;
import org.jsmpp.bean.OptionalParameter.Byte;
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

            // Extract receipted message ID and message state from optional parameters
            String submittedMessageID = extractReceiptedMessageId(deliverSm);
            Integer messageState = extractMessageState(deliverSm);

            // Use messageState from optional parameters as fallback when text parsing returns UNKNOWN
            if (status == DlrEntry.DlrStatus.UNKNOWN && messageState != null) {
                status = DlrFormat.fromMessageState(messageState);
                log.debug("Resolved DLR status from messageState {}: {}", messageState, status);
            }

            // Use submittedMessageID from optional parameters as fallback when text parsing fails
            if (messageId == null && submittedMessageID != null) {
                messageId = submittedMessageID;
                log.debug("Using receipted_message_id from optional parameters: {}", messageId);
            }

            log.info("DLR received - submittedMessageID: {}, messageState: {}, status: {}",
                     submittedMessageID, messageState, status);

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

    private String extractReceiptedMessageId(DeliverSm deliverSm) {
        try {
            OptionalParameter[] optionalParameters = deliverSm.getOptionalParameters();
            if (optionalParameters != null) {
                for (OptionalParameter param : optionalParameters) {
                    if (param.tag == 0x001E) { // receipted_message_id tag
                        if (param instanceof OctetString) {
                            return new String(((OctetString) param).getValue());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract receipted_message_id from optional parameters", e);
        }
        return null;
    }

    private Integer extractMessageState(DeliverSm deliverSm) {
        try {
            OptionalParameter[] optionalParameters = deliverSm.getOptionalParameters();
            if (optionalParameters != null) {
                for (OptionalParameter param : optionalParameters) {
                    if (param.tag == 0x0427) { // message_state tag
                        if (param instanceof Byte) {
                            return (int) ((Byte) param).getValue();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract message_state from optional parameters", e);
        }
        return null;
    }
}
