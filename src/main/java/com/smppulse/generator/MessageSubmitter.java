package com.smppulse.generator;

import com.smppulse.config.DlrConfig;
import com.smppulse.config.LoadTestConfig;
import com.smppulse.config.MessageTemplate;
import com.smppulse.core.session.SmppSessionWrapper;
import com.smppulse.dlr.DlrTracker;
import com.smppulse.metrics.MetricsCollector;
import com.smppulse.param.ParameterResolver;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MessageSubmitter implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(MessageSubmitter.class);

    private final SmppSessionWrapper session;
    private final ParameterResolver parameterResolver;
    private final MetricsCollector metricsCollector;
    private final DlrTracker dlrTracker;
    private final MessageTemplate messageTemplate;
    private final LoadTestConfig loadTestConfig;
    private final DlrConfig dlrConfig;

    public MessageSubmitter(SmppSessionWrapper session, ParameterResolver parameterResolver,
                            MetricsCollector metricsCollector, DlrTracker dlrTracker,
                            MessageTemplate messageTemplate, LoadTestConfig loadTestConfig,
                            DlrConfig dlrConfig) {
        this.session = session;
        this.parameterResolver = parameterResolver;
        this.metricsCollector = metricsCollector;
        this.dlrTracker = dlrTracker;
        this.messageTemplate = messageTemplate;
        this.loadTestConfig = loadTestConfig;
        this.dlrConfig = dlrConfig;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        try {
            String sourceAddr = parameterResolver.resolveTemplate(messageTemplate.getSourceAddress());
            String destAddr = parameterResolver.resolveTemplate(messageTemplate.getDestinationAddress());
            String messageText = parameterResolver.resolveTemplate(messageTemplate.getMessageText());

            byte[] messageBytes = messageText.getBytes();
            byte registeredDelivery = dlrConfig.isRequestDlr() ? dlrConfig.getRegisteredDelivery() : 0;

            metricsCollector.recordSubmitted();

            String messageId = session.submitMessage(
                    sourceAddr, destAddr, messageBytes,
                    registeredDelivery,
                    loadTestConfig.getServiceType(),
                    loadTestConfig.getProtocolId(),
                    loadTestConfig.getPriorityFlag(),
                    loadTestConfig.getDataCodingValue(),
                    loadTestConfig.getValidityPeriod()
            );

            long latency = System.currentTimeMillis() - startTime;
            metricsCollector.recordSuccess(latency);

            if (dlrConfig.isRequestDlr() && messageId != null) {
                dlrTracker.trackMessage(messageId, sourceAddr, destAddr);
            }

        } catch (ResponseTimeoutException e) {
            metricsCollector.recordTimeout();
            log.debug("Submit timeout: {}", e.getMessage());
        } catch (NegativeResponseException e) {
            metricsCollector.recordFailure();
            if (e.getCommandStatus() == 0x58) { // ESME_RTHROTTLED
                metricsCollector.recordThrottle();
            }
            log.debug("Submit failed with status {}: {}", e.getCommandStatus(), e.getMessage());
        } catch (IOException e) {
            metricsCollector.recordFailure();
            log.debug("Submit IO error: {}", e.getMessage());
        } catch (Exception e) {
            metricsCollector.recordFailure();
            log.warn("Unexpected submit error", e);
        }
    }
}
