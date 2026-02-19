package com.smppulse.dlr;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DlrFormat {

    private static final Pattern ID_PATTERN = Pattern.compile("id:([^ ]+)");
    private static final Pattern STAT_PATTERN = Pattern.compile("stat:([A-Z]+)");

    public static String extractMessageId(String dlrText) {
        if (dlrText == null) return null;
        Matcher matcher = ID_PATTERN.matcher(dlrText);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static DlrEntry.DlrStatus extractStatus(String dlrText) {
        if (dlrText == null) return DlrEntry.DlrStatus.UNKNOWN;
        Matcher matcher = STAT_PATTERN.matcher(dlrText);
        if (!matcher.find()) return DlrEntry.DlrStatus.UNKNOWN;

        return switch (matcher.group(1)) {
            case "DELIVRD" -> DlrEntry.DlrStatus.DELIVERED;
            case "EXPIRED" -> DlrEntry.DlrStatus.EXPIRED;
            case "DELETED" -> DlrEntry.DlrStatus.FAILED;
            case "UNDELIV" -> DlrEntry.DlrStatus.FAILED;
            case "ACCEPTD" -> DlrEntry.DlrStatus.PENDING;
            case "REJECTD" -> DlrEntry.DlrStatus.REJECTED;
            default -> DlrEntry.DlrStatus.UNKNOWN;
        };
    }

    public static String formatDlr(String pattern, String messageId, String submitDate,
                                     String doneDate, String stat) {
        return pattern
                .replace("%s", messageId)  // first %s
                .replaceFirst("%s", submitDate)
                .replaceFirst("%s", doneDate)
                .replaceFirst("%s", stat);
    }
}
