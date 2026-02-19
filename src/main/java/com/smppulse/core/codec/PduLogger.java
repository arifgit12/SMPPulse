package com.smppulse.core.codec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class PduLogger {

    private static final Logger log = LoggerFactory.getLogger(PduLogger.class);
    private static final int MAX_RECORDS = 10000;

    private final List<PduRecord> records = new CopyOnWriteArrayList<>();
    private final List<Consumer<PduRecord>> recordListeners = new CopyOnWriteArrayList<>();

    public void logSent(String pduType, String messageId, String sourceAddr, String destAddr) {
        PduRecord record = new PduRecord("OUT", pduType, messageId, sourceAddr, destAddr);
        addRecord(record);
        log.trace(">> {} msgId={} src={} dst={}", pduType, messageId, sourceAddr, destAddr);
    }

    public void logReceived(String pduType, String messageId, String sourceAddr, String destAddr) {
        PduRecord record = new PduRecord("IN", pduType, messageId, sourceAddr, destAddr);
        addRecord(record);
        log.trace("<< {} msgId={} src={} dst={}", pduType, messageId, sourceAddr, destAddr);
    }

    public void logError(String pduType, String messageId, int commandStatus) {
        PduRecord record = new PduRecord("ERR", pduType, messageId, "", "", commandStatus);
        addRecord(record);
        log.trace("!! {} msgId={} status={}", pduType, messageId, commandStatus);
    }

    private void addRecord(PduRecord record) {
        records.add(record);
        if (records.size() > MAX_RECORDS) {
            records.subList(0, records.size() - MAX_RECORDS).clear();
        }
        for (Consumer<PduRecord> listener : recordListeners) {
            listener.accept(record);
        }
    }

    public void addRecordListener(Consumer<PduRecord> listener) {
        recordListeners.add(listener);
    }

    public List<PduRecord> getRecords() {
        return List.copyOf(records);
    }

    public void clear() {
        records.clear();
    }
}
