package com.smppulse.config;

public class RetryPolicy {

    private boolean autoReconnect = true;
    private int initialDelayMs = 1000;
    private int maxDelayMs = 60000;
    private double backoffMultiplier = 2.0;
    private int maxRetries = -1; // -1 = unlimited

    public RetryPolicy() {}

    public boolean isAutoReconnect() { return autoReconnect; }
    public void setAutoReconnect(boolean autoReconnect) { this.autoReconnect = autoReconnect; }

    public int getInitialDelayMs() { return initialDelayMs; }
    public void setInitialDelayMs(int initialDelayMs) { this.initialDelayMs = initialDelayMs; }

    public int getMaxDelayMs() { return maxDelayMs; }
    public void setMaxDelayMs(int maxDelayMs) { this.maxDelayMs = maxDelayMs; }

    public double getBackoffMultiplier() { return backoffMultiplier; }
    public void setBackoffMultiplier(double backoffMultiplier) { this.backoffMultiplier = backoffMultiplier; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public long calculateDelay(int attempt) {
        long delay = (long) (initialDelayMs * Math.pow(backoffMultiplier, attempt));
        return Math.min(delay, maxDelayMs);
    }
}
