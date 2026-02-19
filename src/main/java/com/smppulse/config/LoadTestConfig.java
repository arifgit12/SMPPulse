package com.smppulse.config;

public class LoadTestConfig {

    private int targetTps = 100;
    private int rampUpSeconds = 0;
    private int rampDownSeconds = 0;
    private int durationSeconds = 60;
    private int totalMessages = 0; // 0 = use duration instead
    private String operationType = "SUBMIT_SM";
    private int maxInFlight = 1000;
    private boolean dataCoding0 = true;
    private byte dataCodingValue = 0;
    private int maxMessageLength = 160;
    private String serviceType = "";
    private byte protocolId = 0;
    private byte priorityFlag = 0;
    private String validityPeriod = "";

    public LoadTestConfig() {}

    public int getTargetTps() { return targetTps; }
    public void setTargetTps(int targetTps) { this.targetTps = targetTps; }

    public int getRampUpSeconds() { return rampUpSeconds; }
    public void setRampUpSeconds(int rampUpSeconds) { this.rampUpSeconds = rampUpSeconds; }

    public int getRampDownSeconds() { return rampDownSeconds; }
    public void setRampDownSeconds(int rampDownSeconds) { this.rampDownSeconds = rampDownSeconds; }

    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }

    public int getTotalMessages() { return totalMessages; }
    public void setTotalMessages(int totalMessages) { this.totalMessages = totalMessages; }

    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }

    public int getMaxInFlight() { return maxInFlight; }
    public void setMaxInFlight(int maxInFlight) { this.maxInFlight = maxInFlight; }

    public boolean isDataCoding0() { return dataCoding0; }
    public void setDataCoding0(boolean dataCoding0) { this.dataCoding0 = dataCoding0; }

    public byte getDataCodingValue() { return dataCodingValue; }
    public void setDataCodingValue(byte dataCodingValue) { this.dataCodingValue = dataCodingValue; }

    public int getMaxMessageLength() { return maxMessageLength; }
    public void setMaxMessageLength(int maxMessageLength) { this.maxMessageLength = maxMessageLength; }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    public byte getProtocolId() { return protocolId; }
    public void setProtocolId(byte protocolId) { this.protocolId = protocolId; }

    public byte getPriorityFlag() { return priorityFlag; }
    public void setPriorityFlag(byte priorityFlag) { this.priorityFlag = priorityFlag; }

    public String getValidityPeriod() { return validityPeriod; }
    public void setValidityPeriod(String validityPeriod) { this.validityPeriod = validityPeriod; }
}
