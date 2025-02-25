package com.starling.roundup.util;

import java.util.UUID;

public class IdUtils {

    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }
}