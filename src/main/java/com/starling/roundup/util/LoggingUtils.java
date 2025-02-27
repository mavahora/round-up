package com.starling.roundup.util;

public class LoggingUtils {

    // Only show last 4 digits of accountId
    public static String maskSensitiveData(String accountId) {
        if (accountId == null || accountId.length() < 4) {
            return "****";
        }
        return "****" + accountId.substring(accountId.length() - 4);
    }
}
