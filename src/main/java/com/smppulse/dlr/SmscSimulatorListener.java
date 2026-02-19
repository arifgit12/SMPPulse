package com.smppulse.dlr;

import com.smppulse.config.DlrConfig;
import com.smppulse.metrics.MetricsCollector;
import org.jsmpp.PDUStringException;
import org.jsmpp.bean.*;
import org.jsmpp.extra.ProcessRequestException;
import org.jsmpp.session.*;
import org.jsmpp.util.MessageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

public class SmscSimulatorListener implements ServerMessageReceiverListener {

    private static final Logger log = LoggerFactory.getLogger(SmscSimulatorListener.class);

    private final MetricsCollector metricsCollector;
    private final DlrGenerator dlrGenerator;
    private final DlrConfig dlrConfig;
    private final AtomicLong messageIdCounter = new AtomicLong(1);

    public SmscSimulatorListener(MetricsCollector metricsCollector, DlrGenerator dlrGenerator,
                                  DlrConfig dlrConfig) {
        this.metricsCollector = metricsCollector;
        this.dlrGenerator = dlrGenerator;
        this.dlrConfig = dlrConfig;
    }

    @Override
    public DataSmResult onAcceptDataSm(DataSm dataSm, Session source) throws ProcessRequestException {
        String messageId = String.format("%010d", messageIdCounter.getAndIncrement());
        log.debug("SMSC received data_sm, assigned message_id: {}", messageId);
        try {
            return new DataSmResult(new MessageId(messageId), new OptionalParameter[0]);
        } catch (PDUStringException e) {
            throw new ProcessRequestException("Invalid message ID", 0x00000008, e);
        }
    }

    @Override
    public SubmitSmResult onAcceptSubmitSm(SubmitSm submitSm, SMPPServerSession session)
            throws ProcessRequestException {
        String messageId = String.format("%010d", messageIdCounter.getAndIncrement());
        metricsCollector.recordSubmitted();

        log.debug("SMSC received submit_sm from {} to {}, assigned message_id: {}",
                submitSm.getSourceAddr(), submitSm.getDestAddress(), messageId);

        // Check if DLR is requested
        byte regDelivery = submitSm.getRegisteredDelivery();
        if (regDelivery != 0 && dlrConfig.isGenerateDlr()) {
            dlrGenerator.scheduleDlr(messageId, submitSm.getSourceAddr(),
                    submitSm.getDestAddress(), dlrConfig, (msgId, src, dst, dlrText, success) -> {
                        try {
                            session.deliverShortMessage(
                                    "mc",
                                    TypeOfNumber.INTERNATIONAL,
                                    NumberingPlanIndicator.ISDN,
                                    dst,
                                    TypeOfNumber.INTERNATIONAL,
                                    NumberingPlanIndicator.ISDN,
                                    src,
                                    new ESMClass(MessageMode.DEFAULT, MessageType.SMSC_DEL_RECEIPT,
                                            GSMSpecificFeature.DEFAULT),
                                    (byte) 0,
                                    (byte) 0,
                                    new RegisteredDelivery(0),
                                    DataCodings.ZERO,
                                    dlrText.getBytes()
                            );
                            log.debug("DLR sent for message_id: {}", msgId);
                        } catch (Exception e) {
                            log.warn("Failed to send DLR for {}", msgId, e);
                        }
                    });
        }

        try {
            return new SubmitSmResult(new MessageId(messageId), new OptionalParameter[0]);
        } catch (PDUStringException e) {
            throw new ProcessRequestException("Invalid message ID", 0x00000008, e);
        }
    }

    @Override
    public SubmitMultiResult onAcceptSubmitMulti(SubmitMulti submitMulti, SMPPServerSession session)
            throws ProcessRequestException {
        throw new ProcessRequestException("submit_multi not supported", 0x00000045);
    }

    @Override
    public QuerySmResult onAcceptQuerySm(QuerySm querySm, SMPPServerSession session)
            throws ProcessRequestException {
        throw new ProcessRequestException("query_sm not supported", 0x00000045);
    }

    @Override
    public void onAcceptReplaceSm(ReplaceSm replaceSm, SMPPServerSession session)
            throws ProcessRequestException {
        throw new ProcessRequestException("replace_sm not supported", 0x00000045);
    }

    @Override
    public void onAcceptCancelSm(CancelSm cancelSm, SMPPServerSession session)
            throws ProcessRequestException {
        throw new ProcessRequestException("cancel_sm not supported", 0x00000045);
    }

    @Override
    public BroadcastSmResult onAcceptBroadcastSm(BroadcastSm broadcastSm, SMPPServerSession session)
            throws ProcessRequestException {
        throw new ProcessRequestException("broadcast_sm not supported", 0x00000045);
    }

    @Override
    public void onAcceptCancelBroadcastSm(CancelBroadcastSm cancelBroadcastSm, SMPPServerSession session)
            throws ProcessRequestException {
        throw new ProcessRequestException("cancel_broadcast_sm not supported", 0x00000045);
    }

    @Override
    public QueryBroadcastSmResult onAcceptQueryBroadcastSm(QueryBroadcastSm queryBroadcastSm,
                                                            SMPPServerSession session)
            throws ProcessRequestException {
        throw new ProcessRequestException("query_broadcast_sm not supported", 0x00000045);
    }
}
