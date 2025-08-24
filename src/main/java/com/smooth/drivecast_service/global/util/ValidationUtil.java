package com.smooth.drivecast_service.global.util;

import com.smooth.drivecast_service.model.AlertType;
import com.smooth.drivecast_service.model.AlertEvent;

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
