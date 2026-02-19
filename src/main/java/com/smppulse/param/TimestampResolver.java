package com.smppulse.param;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TimestampResolver {

    public String resolve() {
        return String.valueOf(Instant.now().toEpochMilli());
    }

    public String resolve(String format) {
        if (format == null || format.isEmpty()) {
            return resolve();
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format)
                .withZone(ZoneId.systemDefault());
        return formatter.format(Instant.now());
    }
}
