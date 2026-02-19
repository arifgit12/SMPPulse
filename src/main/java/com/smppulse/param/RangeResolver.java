package com.smppulse.param;

import java.util.concurrent.ThreadLocalRandom;

public class RangeResolver {

    public String resolve(long min, long max) {
        if (min > max) {
            long temp = min;
            min = max;
            max = temp;
        }
        long value = ThreadLocalRandom.current().nextLong(min, max + 1);
        return String.valueOf(value);
    }
}
