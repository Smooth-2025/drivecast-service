package com.smooth.drivecast_service.incident.exception;

import com.smooth.drivecast_service.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum IncidentErrorCode implements ErrorCode {
    // 2101~2149: 클라이언트 오류 (입력/포맷/누락/유효성)
    INVALID_INCIDENT_TYPE(HttpStatus.BAD_REQUEST, 2101, "지원하지 않는 사고 타입입니다."),
    MISSING_LOCATION(HttpStatus.BAD_REQUEST, 2102, "위치 정보는 필수입니다."),
    INVALID_LOCATION_COORDINATES(HttpStatus.BAD_REQUEST, 2103, "위치 좌표가 유효하지 않습니다."),
    MISSING_TIMESTAMP(HttpStatus.BAD_REQUEST, 2104, "타임스탬프는 필수입니다."),
    INVALID_TIMESTAMP_FORMAT(HttpStatus.BAD_REQUEST, 2105, "타임스탬프 형식이 올바르지 않습니다."),
    MISSING_ACCIDENT_ID(HttpStatus.BAD_REQUEST, 2106, "사고 이벤트에는 사고 ID가 필수입니다."),
    INVALID_OBSTACLE_DATA(HttpStatus.BAD_REQUEST, 2107, "장애물 이벤트에는 사고 ID가 있으면 안됩니다."),

    // 2151~2199: 서버 오류 (매핑/처리/전송 실패)
    INCIDENT_EVENT_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 2151, "사고 이벤트 처리 중 오류가 발생했습니다."),
    INCIDENT_MESSAGE_MAPPING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 2152, "사고 메시지 매핑 중 오류가 발생했습니다."),
    INCIDENT_NOTIFICATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 2153, "사고 알림 전송 중 오류가 발생했습니다."),
    VICINITY_SEARCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 2154, "주변 사용자 검색 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final Integer code;
    private final String message;
}
