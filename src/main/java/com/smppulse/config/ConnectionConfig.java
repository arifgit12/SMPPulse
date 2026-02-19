package com.smppulse.config;

public class ConnectionConfig {

    private String host = "localhost";
    private int port = 2775;
    private String systemId = "smppclient";
    private String password = "password";
    private String systemType = "";
    private String bindType = "TRANSCEIVER";
    private String connectionMode = "ESME";
    private byte sourceAddrTon = 0x01;
    private byte sourceAddrNpi = 0x01;
    private byte destAddrTon = 0x01;
    private byte destAddrNpi = 0x01;
    private int windowSize = 10;
    private boolean useTls = false;
    private int enquireLinkInterval = 30000;
    private int responseTimeout = 10000;
    private int sessionCount = 1;
    private RetryPolicy retryPolicy = new RetryPolicy();

    // SMSC simulator settings
    private int smscPort = 2775;

    public ConnectionConfig() {}

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getSystemId() { return systemId; }
    public void setSystemId(String systemId) { this.systemId = systemId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getSystemType() { return systemType; }
    public void setSystemType(String systemType) { this.systemType = systemType; }

    public String getBindType() { return bindType; }
    public void setBindType(String bindType) { this.bindType = bindType; }

    public String getConnectionMode() { return connectionMode; }
    public void setConnectionMode(String connectionMode) { this.connectionMode = connectionMode; }

    public byte getSourceAddrTon() { return sourceAddrTon; }
    public void setSourceAddrTon(byte sourceAddrTon) { this.sourceAddrTon = sourceAddrTon; }

    public byte getSourceAddrNpi() { return sourceAddrNpi; }
    public void setSourceAddrNpi(byte sourceAddrNpi) { this.sourceAddrNpi = sourceAddrNpi; }

    public byte getDestAddrTon() { return destAddrTon; }
    public void setDestAddrTon(byte destAddrTon) { this.destAddrTon = destAddrTon; }

    public byte getDestAddrNpi() { return destAddrNpi; }
    public void setDestAddrNpi(byte destAddrNpi) { this.destAddrNpi = destAddrNpi; }

    public int getWindowSize() { return windowSize; }
    public void setWindowSize(int windowSize) { this.windowSize = windowSize; }

    public boolean isUseTls() { return useTls; }
    public void setUseTls(boolean useTls) { this.useTls = useTls; }

    public int getEnquireLinkInterval() { return enquireLinkInterval; }
    public void setEnquireLinkInterval(int enquireLinkInterval) { this.enquireLinkInterval = enquireLinkInterval; }

    public int getResponseTimeout() { return responseTimeout; }
    public void setResponseTimeout(int responseTimeout) { this.responseTimeout = responseTimeout; }

    public int getSessionCount() { return sessionCount; }
    public void setSessionCount(int sessionCount) { this.sessionCount = sessionCount; }

    public RetryPolicy getRetryPolicy() { return retryPolicy; }
    public void setRetryPolicy(RetryPolicy retryPolicy) { this.retryPolicy = retryPolicy; }

    public int getSmscPort() { return smscPort; }
    public void setSmscPort(int smscPort) { this.smscPort = smscPort; }
}
