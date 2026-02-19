package com.smppulse.param;

import java.util.UUID;

public class UuidResolver {

    public String resolve() {
        return UUID.randomUUID().toString();
    }
}
