package com.smppulse.param;

import java.util.concurrent.ThreadLocalRandom;

public class RandomResolver {

    private static final String DIGITS = "0123456789";
    private static final String ALPHA = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String ALPHANUMERIC = ALPHA + DIGITS;

    public String resolveDigits(int length) {
        return randomString(DIGITS, length);
    }

    public String resolveAlpha(int length) {
        return randomString(ALPHA, length);
    }

    public String resolveAlphanumeric(int length) {
        return randomString(ALPHANUMERIC, length);
    }

    private String randomString(String chars, int length) {
        if (length <= 0) length = 1;
        StringBuilder sb = new StringBuilder(length);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
