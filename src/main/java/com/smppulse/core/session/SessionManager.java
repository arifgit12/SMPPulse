package com.smppulse.core.session;

import com.smppulse.config.ConnectionConfig;
import com.smppulse.core.codec.PduLogger;
import com.smppulse.dlr.DlrHandler;
import com.smppulse.dlr.DlrTracker;
import com.smppulse.metrics.MetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

    private final MetricsCollector metricsCollector;
    private final DlrTracker dlrTracker;
    private final PduLogger pduLogger = new PduLogger();
    private final List<SmppSessionWrapper> sessions = new CopyOnWriteArrayList<>();
    private final List<SessionEventListener> listeners = new CopyOnWriteArrayList<>();
    private final List<AutoReconnectHandler> reconnectHandlers = new ArrayList<>();
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);

    public SessionManager(MetricsCollector metricsCollector, DlrTracker dlrTracker) {
        this.metricsCollector = metricsCollector;
        this.dlrTracker = dlrTracker;
    }

    public void addListener(SessionEventListener listener) {
        listeners.add(listener);
    }

    public void connectAll(ConnectionConfig config) throws IOException {
        disconnectAll();

        int sessionCount = Math.max(1, config.getSessionCount());
        log.info("Creating {} SMPP sessions to {}:{}", sessionCount, config.getHost(), config.getPort());

        for (int i = 0; i < sessionCount; i++) {
            String sessionId = "session-" + (i + 1);
            SmppSessionWrapper wrapper = new SmppSessionWrapper(sessionId, config, pduLogger);

            DlrHandler dlrHandler = new DlrHandler(dlrTracker, metricsCollector);
            wrapper.setDlrHandler(dlrHandler);

            for (SessionEventListener listener : listeners) {
                wrapper.addListener(listener);
            }

            if (config.getRetryPolicy().isAutoReconnect()) {
                AutoReconnectHandler reconnectHandler = new AutoReconnectHandler(wrapper, config.getRetryPolicy());
                wrapper.addListener(reconnectHandler);
                reconnectHandlers.add(reconnectHandler);
            }

            wrapper.connect();
            sessions.add(wrapper);
        }

        log.info("All {} sessions connected", sessionCount);
    }

    public SmppSessionWrapper getNextSession() {
        if (sessions.isEmpty()) {
            return null;
        }
        int index = roundRobinIndex.getAndUpdate(i -> (i + 1) % sessions.size());
        return sessions.get(index);
    }

    public void disconnectAll() {
        for (AutoReconnectHandler handler : reconnectHandlers) {
            handler.stop();
        }
        reconnectHandlers.clear();

        for (SmppSessionWrapper session : sessions) {
            session.disconnect();
        }
        sessions.clear();
        roundRobinIndex.set(0);
        log.info("All sessions disconnected");
    }

    public boolean hasConnectedSession() {
        return sessions.stream().anyMatch(SmppSessionWrapper::isConnected);
    }

    public int getConnectedCount() {
        return (int) sessions.stream().filter(SmppSessionWrapper::isConnected).count();
    }

    public int getTotalCount() {
        return sessions.size();
    }

    public List<SmppSessionWrapper> getSessions() {
        return List.copyOf(sessions);
    }

    public PduLogger getPduLogger() {
        return pduLogger;
    }
}
