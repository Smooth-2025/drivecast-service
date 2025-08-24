package com.smooth.drivecast_service.global.util;

import com.smooth.drivecast_service.model.AlertType;
import com.smooth.drivecast_service.model.AlertEvent;

/**
 * 공통 검증 유틸리티
 * 정책 없는 순수 검증 로직만 포함
 **/
public class ValidationUtil {

    /**
     * AlertEvent 검증
     * @param event 검증할 이벤트
     * @throws IllegalArgumentException 검증 실패 시
     **/
    public static void validateAlertEvent(AlertEvent event) {
        if (event.type() == null || event.type().isBlank()) {
            throw new IllegalArgumentException("type is required");
        }

        AlertType type = AlertType.from(event.type())
                .orElseThrow(() -> new IllegalArgumentException("invalid alert type: " + event.type()));

        switch (type) {
            case ACCIDENT -> {
                require(event.accidentId(), "accidentId is required for type=accident");
                require(event.userId(), "userId(vehicleId) is required for type=accident");
                require(event.latitude(), "latitude is required for type=accident");
                require(event.longitude(), "longitude is required for type=accident");
                require(event.timestamp(), "timestamp is required for type=accident");
            }
            case OBSTACLE -> {
                require(event.latitude(), "latitude is required for type=obstacle");
                require(event.longitude(), "longitude is required for type=obstacle");
                require(event.timestamp(), "timestamp is required for type=obstacle");
            }
            case START, END -> {
                require(event.userId(), "userId(vehicleId) is required for type=" + type);
                require(event.timestamp(), "timestamp is required for type=" + type);
            }
        }
    }

    /**
     * 좌표 유효성 검증
     *
     * @param latitude  위도
     * @param longitude 경도
     * @return 유효하면 true
     **/
    public static boolean isValidCoordinate(Double latitude, Double longitude) {
        if(latitude == null || longitude == null) return false;
        return latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180;
    }

    /**
     * 사용자 ID 유효성 검증 (null 허용)
     *
     * @param userId 사용자 ID
     * @return 유효하면 true (null 허용)
     **/
    public static boolean isValidUserId(String userId) {
        return userId == null || !userId.isBlank();
    }

    /**
     * 문자열 필수 값 검증
     * @param value 검증할 값
     * @param message 오류 메세지
     **/
    private static void require(Object value, String message) {
        if (value == null || (value instanceof String str && str.isBlank())) {
            throw new IllegalArgumentException(message);
        }
    }
}
