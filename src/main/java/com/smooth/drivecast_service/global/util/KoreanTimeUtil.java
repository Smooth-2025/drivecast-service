package com.smooth.drivecast_service.global.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class KoreanTimeUtil {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter LOCATION_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static String toLocationKey(Instant instant) {
        ZonedDateTime koreanTime = instant.atZone(KOREA_ZONE);
        String timeString = koreanTime.format(LOCATION_KEY_FORMATTER);
        return "location:" + timeString;
    }
}