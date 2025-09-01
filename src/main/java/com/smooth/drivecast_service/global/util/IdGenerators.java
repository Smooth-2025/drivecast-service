package com.smooth.drivecast_service.global.util;

import com.smooth.drivecast_service.driving.dto.DrivingEvent;
import com.smooth.drivecast_service.incident.dto.IncidentEvent;

import java.util.Optional;

/**
 * 도메인별 ID 생성 유틸리티
 * 정책 없는 순수 ID 생성 로직만 포함
 **/

public class IdGenerators {

    /**
     * 주행 이벤트로부터 alertId 생성
     **/
    public static String generateDrivingAlertId(DrivingEvent event) {
        return switch (event.type()) {
            case START -> String.format("start-%s-%s",
                    event.userId(),
                    sanitizeTimestamp(event.timestamp()));
            case END -> String.format("end-%s-%s",
                    event.userId(),
                    sanitizeTimestamp(event.timestamp()));
        };
    }

    /**
     * 사고 이벤트로부터 alertId 생성
     * IncidentEvent 생성자에서 이미 필수 검증을 하므로 항상 유효한 ID 반환
     **/
    public static String generateIncidentAlertId(IncidentEvent event) {
        return switch (event.type()) {
            case ACCIDENT -> event.accidentId(); // 이미 검증됨
            case OBSTACLE -> String.format("obstacle-%s-%s-%s",
                    formatLatitude(event.latitude()),
                    formatLongitude(event.longitude()),
                    sanitizeTimestamp(event.timestamp()));
        };
    }

    /**
     * 주행 세션 ID 생성
     * @param userId 사용자 ID
     * @param timestamp 시각
     * @return 세션 ID
     **/
    public static String generateSessionId(String userId, String timestamp) {
        return "session-" + userId + "-" + sanitizeTimestamp(timestamp);
    }

    /**
     * 위도를 ID용 문자열로 포맷팅 (N/S 구분)
     * 37.52342 -> "N3752342", -37.123 -> "S371230"
     **/
    private static String formatLatitude(Double latitude) {
        if(latitude == null) return "0";
        
        String prefix = latitude >= 0 ? "N" : "S";
        String absValue = String.valueOf(Math.abs(latitude)).replace(".", "");
        return prefix + absValue;
    }

    /**
     * 경도를 ID용 문자열로 포맷팅 (E/W 구분)
     * 127.123 -> "E1271230", -127.123 -> "W1271230"
     **/
    private static String formatLongitude(Double longitude) {
        if(longitude == null) return "0";
        
        String prefix = longitude >= 0 ? "E" : "W";
        String absValue = String.valueOf(Math.abs(longitude)).replace(".", "");
        return prefix + absValue;
    }

    /**
     * 타임스탬프를 ID용 문자열로 변환
     * "2025-08-01T17:03:00" -> "20250804170300" */
    private static String sanitizeTimestamp(String timestamp) {
        if(timestamp == null) return "unknown";
        return timestamp.replace(":", "").replace("-", "").replace("T", "");
    }
}