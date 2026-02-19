package com.smppulse.param;

import java.util.concurrent.atomic.AtomicLong;

public class SequenceResolver {

    private final AtomicLong counter = new AtomicLong(1);

    public String resolve() {
        return String.valueOf(counter.getAndIncrement());
    }

    public void reset() {
        counter.set(1);
    }

    public long current() {
        return counter.get();
    }
}
