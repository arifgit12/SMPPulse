package com.smppulse.config;

import java.util.HashMap;
import java.util.Map;

public class MessageTemplate {

    private String sourceAddress = "${random.digits(10)}";
    private String destinationAddress = "${random.digits(10)}";
    private String messageText = "Test message ${sequence} at ${timestamp}";
    private Map<String, String> tlvValues = new HashMap<>();

    public MessageTemplate() {}

    public String getSourceAddress() { return sourceAddress; }
    public void setSourceAddress(String sourceAddress) { this.sourceAddress = sourceAddress; }

    public String getDestinationAddress() { return destinationAddress; }
    public void setDestinationAddress(String destinationAddress) { this.destinationAddress = destinationAddress; }

    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }

    public Map<String, String> getTlvValues() { return tlvValues; }
    public void setTlvValues(Map<String, String> tlvValues) { this.tlvValues = tlvValues; }
}
