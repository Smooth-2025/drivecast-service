package com.smooth.drivecast_service.global.util;

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
    private static final DateTimeFormatter KOREAN_FORMATTER_NO_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final DateTimeFormatter LOCATION_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 한국시 문자열을 Instant로 변환
     * 입력: "2025-08-04T17:03:00" 또는 "2025-08-04T17:03" (한국시)
     * 출력: Instant (UTC)
     */
    public static Instant parseKoreanTime(String koreanTimeString) {
        try {
            LocalDateTime localDateTime;
            
            // 먼저 초가 포함된 형태로 파싱 시도
            try {
                localDateTime = LocalDateTime.parse(koreanTimeString, KOREAN_FORMATTER);
            } catch (Exception e) {
                // 초가 없는 형태로 파싱 시도
                localDateTime = LocalDateTime.parse(koreanTimeString, KOREAN_FORMATTER_NO_SECONDS);
            }
            
            ZonedDateTime koreanTime = localDateTime.atZone(KOREA_ZONE);
            return koreanTime.toInstant();
        } catch (Exception e) {
            throw new IllegalArgumentException("한국시 파싱 실패: " + koreanTimeString, e);
        }
    }

    /**
     * Incident 전용 - 초단위까지 필수인 한국시 문자열을 Instant로 변환
     * 입력: "2025-08-04T17:03:00" (한국시, 초 필수)
     * 출력: Instant (UTC)
     */
    public static Instant parseKoreanTimeWithSeconds(String koreanTimeString) {
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(koreanTimeString, KOREAN_FORMATTER);
            ZonedDateTime koreanTime = localDateTime.atZone(KOREA_ZONE);
            return koreanTime.toInstant();
        } catch (Exception e) {
            throw new IllegalArgumentException("한국시 파싱 실패 (초단위 필수): " + koreanTimeString, e);
        }
    }

    /**
     * UTC Instant를 KST 기반 location 키로 변환
     * @param instant UTC 시각
     * @return location 키 (예: location:20250822143000)
     **/
    public static String toLocationKey(Instant instant) {
        ZonedDateTime koreanTime = instant.atZone(KOREA_ZONE);
        String timeString = koreanTime.format(LOCATION_KEY_FORMATTER);
        return "location:" + timeString;
    }

    /**
     * 현재 시각의 location 키 생성
     * @return 현재 location 키
     **/
    public static String getCurrentLocationKey() {
        return toLocationKey(Instant.now());
    }

    /**
     * KST 문자열을 location 키로 변환
     * @param koreanTimeString KST 문자열 (예: "2025-08-22T14:30:00")
     * @return location 키
     **/
    public static String koreanTimeToLocationKey(String koreanTimeString) {
        LocalDateTime localDateTime = LocalDateTime.parse(koreanTimeString);
        ZonedDateTime koreanTime = localDateTime.atZone(KOREA_ZONE);
        String timeString = koreanTime.format(LOCATION_KEY_FORMATTER);
        return "location:" + timeString;
    }
}