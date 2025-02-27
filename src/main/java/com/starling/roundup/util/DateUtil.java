package com.starling.roundup.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class DateUtil {

    private static final String STARLING_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmmss");

    public static String toStarlingDateFormat(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern(STARLING_DATE_FORMAT));
    }

    public static String toStarlingEndDate(LocalDate weekCommencing) {
        return toStarlingDateFormat(weekCommencing.plusDays(6));
    }

    public static String getCurrentDateTime() {
        LocalDateTime localDateTime = LocalDateTime.now();
        return localDateTime.format(DATE_FORMAT) + localDateTime.format(TIME_FORMAT);
    }
}
