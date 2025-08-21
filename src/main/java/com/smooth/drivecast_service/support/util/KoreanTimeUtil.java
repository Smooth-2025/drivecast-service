package com.smooth.drivecast_service.support.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 한국시 기준 시간 처리 유틸리티
 */
public class KoreanTimeUtil {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter KOREAN_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter LOCATION_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 한국시 문자열을 Instant로 변환
     * 입력: "2025-08-04T17:03:00" (한국시)
     * 출력: Instant (UTC)
     */
    public static Instant parseKoreanTime(String koreanTimeString) {
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(koreanTimeString, KOREAN_FORMATTER);
            ZonedDateTime koreanTime = localDateTime.atZone(KOREA_ZONE);
            return koreanTime.toInstant();
        } catch (Exception e) {
            throw new IllegalArgumentException("한국시 파싱 실패: " + koreanTimeString, e);
        }
    }

    /**
     * Instant를 한국시 기준 위치 키 형식으로 변환
     * 입력: Instant
     * 출력: "location:20250801111045" (한국시 기준)
     */
    public static String toLocationKey(Instant instant) {
        ZonedDateTime koreanTime = instant.atZone(KOREA_ZONE);
        String timeString = koreanTime.format(LOCATION_KEY_FORMATTER);
        return "location:" + timeString;
    }

    /**
     * 현재 한국시 기준 위치 키 생성
     */
    public static String getCurrentLocationKey() {
        return toLocationKey(Instant.now());
    }

    /**
     * 한국시 문자열을 위치 키로 직접 변환
     * 입력: "2025-08-04T17:03:00"
     * 출력: "location:20250804170300"
     */
    public static String koreanTimeToLocationKey(String koreanTimeString) {
        Instant instant = parseKoreanTime(koreanTimeString);
        return toLocationKey(instant);
    }
}