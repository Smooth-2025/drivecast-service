package com.smooth.drivecast_service.global.util;

import com.smooth.drivecast_service.model.AlertEvent;

import java.util.Optional;

/**
 * 도메인별 ID 생성 유틸리티
 * 정책 없는 순수 ID 생성 로직만 포함
 **/

public class IdGenerators {

    /**
     * AlertEvent 로 부터 alertId 생성
     * @param event 알림 이벤트
     * @return 생성된 alertId
     **/
    public static Optional<String> generateAlertId(AlertEvent event) {
        return switch (event.type()) {
            case "accident" -> Optional.ofNullable(event.accidentId());
            case "obstacle" -> {
                String obstacleId = String.format("obstacle-%s-%s-%s",
                        formatCoordinate(event.latitude()),
                        formatCoordinate(event.longitude()),
                        sanitizeTimestamp(event.timestamp()));
                yield Optional.of(obstacleId);
            }
            case "start" -> {
                String startId = String.format("start-%s-%s",
                        event.userId(),
                        sanitizeTimestamp(event.timestamp()));
                yield Optional.of(startId);
            }
            case "end" -> {
                String endId = String.format("end-%s-%s",
                        event.userId(),
                        sanitizeTimestamp(event.timestamp()));
                yield Optional.of(endId);
            }
            default -> Optional.empty();
        };
    }

    /**
     * 중복 방지 키 생성
     * @param alertId 알림 ID
     * @param userId  사용자 ID (OBSTACLE은 null 가능)
     * @return 중복 방지 키
     **/
    public static String generateDedupKey(String alertId, String userId) {
        return "alert:" + alertId + ":" + (userId != null ? userId : "anonymous");
    }

    /**
     * 주행 세션 ID 생성
     *
     * @param userId    사용자 ID
     * @param timestamp 시각
     * @return 세션 ID
     **/
    public static String generateSessionId(String userId, String timestamp) {
        return "session-" + userId + "-" + sanitizeTimestamp(timestamp);
    }

    /**
     * 좌표를 ID용 문자열로 포맷팅
     * 37.52342 -> "3752342"
     **/
    private static String formatCoordinate(Double coordinate) {
        if(coordinate == null) return "0";
        return coordinate.toString().replace(".", "").replace("-", "");
    }

    /**
     * 타임스탬프를 ID용 문자열로 변환
     * "2025-08-01T17:03:00" -> "20250804170300" */
    private static String sanitizeTimestamp(String timestamp) {
        if(timestamp == null) return "unknown";
        return timestamp.replace(":", "").replace("-", "").replace("T", "");
    }
}