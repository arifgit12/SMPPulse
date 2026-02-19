package com.smppulse.util;

public final class SmppUtils {

    private SmppUtils() {}

    public static String commandStatusToString(int status) {
        return switch (status) {
            case 0x00000000 -> "ESME_ROK";
            case 0x00000001 -> "ESME_RINVMSGLEN";
            case 0x00000002 -> "ESME_RINVCMDLEN";
            case 0x00000003 -> "ESME_RINVCMDID";
            case 0x00000004 -> "ESME_RINVBNDSTS";
            case 0x00000005 -> "ESME_RALYBND";
            case 0x00000006 -> "ESME_RINVPRTFLG";
            case 0x00000007 -> "ESME_RINVREGDLVFLG";
            case 0x00000008 -> "ESME_RSYSERR";
            case 0x0000000D -> "ESME_RINVSRCADR";
            case 0x0000000E -> "ESME_RINVDSTADR";
            case 0x0000000F -> "ESME_RINVMSGID";
            case 0x00000011 -> "ESME_RBINDFAIL";
            case 0x00000013 -> "ESME_RINVPASWD";
            case 0x00000014 -> "ESME_RINVSYSID";
            case 0x00000045 -> "ESME_RSUBMITFAIL";
            case 0x00000058 -> "ESME_RTHROTTLED";
            case 0x00000061 -> "ESME_RINVSCHED";
            case 0x00000062 -> "ESME_RINVEXPIRY";
            case 0x000000FE -> "ESME_RDELIVERYFAILURE";
            default -> String.format("0x%08X", status);
        };
    }

    public static boolean isValidAddress(String address) {
        return address != null && !address.isEmpty() && address.length() <= 21;
    }

    public static boolean isValidPort(int port) {
        return port > 0 && port <= 65535;
    }

    public static boolean isValidSystemId(String systemId) {
        return systemId != null && !systemId.isEmpty() && systemId.length() <= 16;
    }

    public static String formatMessageId(String messageId) {
        if (messageId == null) return "null";
        // Remove leading zeros for display
        return messageId.replaceFirst("^0+", "");
    }
}
