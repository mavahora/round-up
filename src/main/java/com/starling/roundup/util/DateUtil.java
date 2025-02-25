package com.starling.roundup.util;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class DateUtil {

    private static final String STARLING_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static String toStarlingDateFormat(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern(STARLING_DATE_FORMAT));
    }

    public static String toStarlingEndDate(LocalDate weekCommencing) {
        return toStarlingDateFormat(weekCommencing.plusDays(6));
    }
}
