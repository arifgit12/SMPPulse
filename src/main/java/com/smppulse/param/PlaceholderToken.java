package com.smppulse.param;

public record PlaceholderToken(
    String fullExpression,
    String function,
    String[] args,
    int startIndex,
    int endIndex
) {
    public boolean hasArgs() {
        return args != null && args.length > 0;
    }

    public String getArg(int index) {
        if (args == null || index >= args.length) return "";
        return args[index];
    }

    public int getIntArg(int index, int defaultValue) {
        try {
            return Integer.parseInt(getArg(index).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public long getLongArg(int index, long defaultValue) {
        try {
            return Long.parseLong(getArg(index).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
