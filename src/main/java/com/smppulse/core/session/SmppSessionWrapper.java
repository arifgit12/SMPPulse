package com.smppulse.core.session;

import com.smppulse.config.ConnectionConfig;
import com.smppulse.core.codec.PduLogger;
import com.smppulse.dlr.DlrHandler;
import org.jsmpp.bean.*;
import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.jsmpp.extra.SessionState;
import org.jsmpp.session.*;
import org.jsmpp.util.MessageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SmppSessionWrapper {

    private static final Logger log = LoggerFactory.getLogger(SmppSessionWrapper.class);

    private final String id;
    private final ConnectionConfig config;
    private final PduLogger pduLogger;
    private SMPPSession session;
    private volatile SessionEventListener.ConnectionState state = SessionEventListener.ConnectionState.DISCONNECTED;
    private final List<SessionEventListener> listeners = new CopyOnWriteArrayList<>();
    private DlrHandler dlrHandler;

    public SmppSessionWrapper(String id, ConnectionConfig config, PduLogger pduLogger) {
        this.id = id;
        this.config = config;
        this.pduLogger = pduLogger;
    }

    public void addListener(SessionEventListener listener) {
        listeners.add(listener);
    }

    public void setDlrHandler(DlrHandler dlrHandler) {
        this.dlrHandler = dlrHandler;
    }

    public void connect() throws IOException {
        setState(SessionEventListener.ConnectionState.CONNECTING);
        try {
            session = new SMPPSession();
            session.setEnquireLinkTimer(config.getEnquireLinkInterval());
            session.setTransactionTimer(config.getResponseTimeout());
            session.setPduProcessorDegree(3);

            session.setMessageReceiverListener(new MessageReceiverListener() {
                @Override
                public void onAcceptDeliverSm(DeliverSm deliverSm) {
                    pduLogger.logReceived("deliver_sm", deliverSm.getId(),
                            deliverSm.getSourceAddr(), deliverSm.getDestAddress());

                    if (MessageType.SMSC_DEL_RECEIPT.containedIn(deliverSm.getEsmClass())) {
                        if (dlrHandler != null) {
                            dlrHandler.handleDlr(deliverSm);
                        }
                    } else {
                        for (SessionEventListener listener : listeners) {
                            listener.onMessageReceived(SmppSessionWrapper.this,
                                    null, deliverSm.getSourceAddr(),
                                    deliverSm.getDestAddress(),
                                    new String(deliverSm.getShortMessage()));
                        }
                    }
                }

                @Override
                public void onAcceptAlertNotification(AlertNotification alertNotification) {
                    log.debug("Alert notification received");
                }

                @Override
                public DataSmResult onAcceptDataSm(DataSm dataSm, Session source) {
                    pduLogger.logReceived("data_sm",
                            String.valueOf(dataSm.getSequenceNumber()),
                            dataSm.getSourceAddr(), dataSm.getDestAddress());
                    try {
                        return new DataSmResult(new MessageId("0"), new OptionalParameter[0]);
                    } catch (Exception e) {
                        return null;
                    }
                }
            });

            BindType bindType = BindType.valueOf(config.getBindType());
            String systemId;
            switch (bindType) {
                case TRANSMITTER:
                    systemId = session.connectAndBind(
                            config.getHost(), config.getPort(),
                            new BindParameter(
                                    org.jsmpp.bean.BindType.BIND_TX,
                                    config.getSystemId(), config.getPassword(),
                                    config.getSystemType(),
                                    TypeOfNumber.valueOf(config.getSourceAddrTon()),
                                    NumberingPlanIndicator.valueOf(config.getSourceAddrNpi()),
                                    null
                            )
                    );
                    break;
                case RECEIVER:
                    systemId = session.connectAndBind(
                            config.getHost(), config.getPort(),
                            new BindParameter(
                                    org.jsmpp.bean.BindType.BIND_RX,
                                    config.getSystemId(), config.getPassword(),
                                    config.getSystemType(),
                                    TypeOfNumber.valueOf(config.getSourceAddrTon()),
                                    NumberingPlanIndicator.valueOf(config.getSourceAddrNpi()),
                                    null
                            )
                    );
                    break;
                default:
                    systemId = session.connectAndBind(
                            config.getHost(), config.getPort(),
                            new BindParameter(
                                    org.jsmpp.bean.BindType.BIND_TRX,
                                    config.getSystemId(), config.getPassword(),
                                    config.getSystemType(),
                                    TypeOfNumber.valueOf(config.getSourceAddrTon()),
                                    NumberingPlanIndicator.valueOf(config.getSourceAddrNpi()),
                                    null
                            )
                    );
                    break;
            }

            log.info("Session {} bound as {} with system_id: {}", id, bindType, systemId);
            setState(SessionEventListener.ConnectionState.CONNECTED);

        } catch (IOException e) {
            setState(SessionEventListener.ConnectionState.ERROR);
            throw e;
        }
    }

    public String submitMessage(String sourceAddr, String destAddr, byte[] message,
                                byte registeredDelivery, String serviceType,
                                byte protocolId, byte priorityFlag,
                                byte dataCoding, String validityPeriod)
            throws ResponseTimeoutException, NegativeResponseException, IOException {

        if (session == null || (!session.getSessionState().isTransmittable())) {
            throw new IOException("Session not bound for transmitting");
        }

        try {
            SubmitSmResult result = session.submitShortMessage(
                    serviceType,
                    TypeOfNumber.valueOf(config.getSourceAddrTon()),
                    NumberingPlanIndicator.valueOf(config.getSourceAddrNpi()),
                    sourceAddr,
                    TypeOfNumber.valueOf(config.getDestAddrTon()),
                    NumberingPlanIndicator.valueOf(config.getDestAddrNpi()),
                    destAddr,
                    new ESMClass(),
                    protocolId,
                    priorityFlag,
                    null,
                    (validityPeriod != null && !validityPeriod.isEmpty()) ? validityPeriod : null,
                    new RegisteredDelivery(registeredDelivery),
                    (byte) 0,
                    DataCodings.newInstance(dataCoding),
                    (byte) 0,
                    message
            );

            String messageId = result.getMessageId();
            pduLogger.logSent("submit_sm", messageId, sourceAddr, destAddr);
            return messageId;
        } catch (PDUException | InvalidResponseException e) {
            throw new IOException("PDU error: " + e.getMessage(), e);
        }
    }

    public void disconnect() {
        if (session != null) {
            try {
                session.unbindAndClose();
            } catch (Exception e) {
                log.warn("Error disconnecting session {}", id, e);
            }
        }
        setState(SessionEventListener.ConnectionState.DISCONNECTED);
    }

    public boolean isConnected() {
        return session != null && session.getSessionState().isBound();
    }

    private void setState(SessionEventListener.ConnectionState newState) {
        this.state = newState;
        for (SessionEventListener listener : listeners) {
            listener.onConnectionStateChanged(this, newState);
        }
    }

    public String getId() { return id; }
    public SessionEventListener.ConnectionState getState() { return state; }
    public ConnectionConfig getConfig() { return config; }
}
