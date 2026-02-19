package com.smppulse.config;

public class DlrConfig {

    private boolean requestDlr = false;
    private byte registeredDelivery = 0x01;
    private int dlrTimeoutSeconds = 60;

    // SMSC simulator DLR settings
    private boolean generateDlr = true;
    private int dlrDelayMs = 1000;
    private double dlrSuccessRate = 0.95;
    private String dlrFormat = "id:%s sub:001 dlvrd:001 submit date:%s done date:%s stat:%s err:000 text:";

    public DlrConfig() {}

    public boolean isRequestDlr() { return requestDlr; }
    public void setRequestDlr(boolean requestDlr) { this.requestDlr = requestDlr; }

    public byte getRegisteredDelivery() { return registeredDelivery; }
    public void setRegisteredDelivery(byte registeredDelivery) { this.registeredDelivery = registeredDelivery; }

    public int getDlrTimeoutSeconds() { return dlrTimeoutSeconds; }
    public void setDlrTimeoutSeconds(int dlrTimeoutSeconds) { this.dlrTimeoutSeconds = dlrTimeoutSeconds; }

    public boolean isGenerateDlr() { return generateDlr; }
    public void setGenerateDlr(boolean generateDlr) { this.generateDlr = generateDlr; }

    public int getDlrDelayMs() { return dlrDelayMs; }
    public void setDlrDelayMs(int dlrDelayMs) { this.dlrDelayMs = dlrDelayMs; }

    public double getDlrSuccessRate() { return dlrSuccessRate; }
    public void setDlrSuccessRate(double dlrSuccessRate) { this.dlrSuccessRate = dlrSuccessRate; }

    public String getDlrFormat() { return dlrFormat; }
    public void setDlrFormat(String dlrFormat) { this.dlrFormat = dlrFormat; }
}
