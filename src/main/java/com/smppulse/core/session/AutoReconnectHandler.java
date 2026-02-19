package com.smppulse.core.session;

import com.smppulse.config.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AutoReconnectHandler implements SessionEventListener {

    private static final Logger log = LoggerFactory.getLogger(AutoReconnectHandler.class);

    private final SmppSessionWrapper session;
    private final RetryPolicy retryPolicy;
    private final AtomicInteger attempts = new AtomicInteger(0);
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual().name("reconnect-", 0).factory()
    );

    public AutoReconnectHandler(SmppSessionWrapper session, RetryPolicy retryPolicy) {
        this.session = session;
        this.retryPolicy = retryPolicy;
    }

    @Override
    public void onConnectionStateChanged(SmppSessionWrapper sess, ConnectionState newState) {
        if (stopped.get()) return;

        if (newState == ConnectionState.DISCONNECTED || newState == ConnectionState.ERROR) {
            if (!reconnecting.get()) {
                scheduleReconnect();
            }
        } else if (newState == ConnectionState.CONNECTED) {
            attempts.set(0);
            reconnecting.set(false);
        }
    }

    @Override
    public void onSessionError(SmppSessionWrapper session, Exception error) {
        log.warn("Session {} error: {}", session.getId(), error.getMessage());
    }

    @Override
    public void onMessageReceived(SmppSessionWrapper session, String messageId,
                                   String sourceAddr, String destAddr, String message) {
        // No action needed
    }

    private void scheduleReconnect() {
        if (stopped.get()) return;

        int attempt = attempts.getAndIncrement();
        if (retryPolicy.getMaxRetries() >= 0 && attempt >= retryPolicy.getMaxRetries()) {
            log.warn("Max reconnect attempts ({}) reached for session {}", retryPolicy.getMaxRetries(), session.getId());
            return;
        }

        long delayMs = retryPolicy.calculateDelay(attempt);
        log.info("Scheduling reconnect for session {} in {}ms (attempt {})", session.getId(), delayMs, attempt + 1);

        reconnecting.set(true);
        scheduler.schedule(() -> {
            if (stopped.get()) return;
            try {
                log.info("Attempting reconnect for session {}", session.getId());
                session.connect();
            } catch (Exception e) {
                log.warn("Reconnect failed for session {}: {}", session.getId(), e.getMessage());
                scheduleReconnect();
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        stopped.set(true);
        reconnecting.set(false);
        scheduler.shutdownNow();
    }
}
