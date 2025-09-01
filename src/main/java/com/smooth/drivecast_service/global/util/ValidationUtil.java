package com.smooth.drivecast_service.global.util;

/**
 * 공통 검증 유틸리티 (정책 없는 순수 검증 로직만)
 * 가이드: global/util - 시간/검증/ID 등
 **/
public final class ValidationUtil {

    private ValidationUtil() {
        throw new UnsupportedOperationException("유틸리티 클래스입니다.");
    }

    /**
     * 좌표 유효성 검증 (정책 없음)
     * @param latitude  위도
     * @param longitude 경도
     * @return 유효하면 true
     **/
    public static boolean isValidCoordinate(Double latitude, Double longitude) {
        if(latitude == null || longitude == null) return false;
        return latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180;
    }

    /**
     * 사용자 ID 유효성 검증 (정책 없음)
     * @param userId 사용자 ID
     * @return 유효하면 true (null 허용)
     **/
    public static boolean isValidUserId(String userId) {
        return userId == null || !userId.isBlank();
    }

    /**
     * 문자열 공백 검증 (정책 없음)
     * @param value 검증할 문자열
     * @return 유효하면 true
     **/
    public static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 타임스탬프 형식 기본 검증 (정책 없음)
     * @param timestamp 타임스탬프 문자열
     * @return 기본 형식이 유효하면 true
     **/
    public static boolean hasValidTimestampFormat(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) return false;
        return timestamp.length() >= 10;
    }

    /**
     * Incident 전용 엄격한 타임스탬프 검증 (초단위 필수)
     * @param timestamp 타임스탬프 문자열
     * @return "yyyy-MM-ddTHH:mm:ss" 형식이면 true
     **/
    public static boolean hasValidIncidentTimestampFormat(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) return false;
        // "2025-08-27T09:23:45" 형태 (19자) 체크
        if (timestamp.length() != 19) return false;
        // 기본 패턴 체크: yyyy-MM-ddTHH:mm:ss
        return timestamp.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
    }

    /**
     * 숫자 범위 검증 (정책 없음)
     * @param value 검증할 값
     * @param min 최소값
     * @param max 최대값
     * @return 범위 내에 있으면 true
     **/
    public static boolean isInRange(Double value, double min, double max) {
        if (value == null) return false;
        return value >= min && value <= max;
    }
}
