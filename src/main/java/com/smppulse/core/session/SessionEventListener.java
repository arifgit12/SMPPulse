package com.smppulse.core.session;

public interface SessionEventListener {

    enum ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, ERROR
    }

    void onConnectionStateChanged(SmppSessionWrapper session, ConnectionState newState);

    void onSessionError(SmppSessionWrapper session, Exception error);

    void onMessageReceived(SmppSessionWrapper session, String messageId, String sourceAddr, String destAddr, String message);
}
