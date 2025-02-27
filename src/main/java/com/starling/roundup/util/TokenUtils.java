package com.starling.roundup.util;

/**
 * Utility class to return the token without the Bearer prefix.
 */
public class TokenUtils {

    private static final String BEARER_PREFIX = "Bearer ";

    public static String extractToken(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(7);
        }
        return bearerToken;
    }
}