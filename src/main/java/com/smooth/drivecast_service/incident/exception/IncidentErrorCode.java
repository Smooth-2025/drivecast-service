package com.smooth.drivecast_service.incident.exception;

import com.smooth.drivecast_service.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum IncidentErrorCode implements ErrorCode {
    // 2100번대: 사고 도메인 에러
    INVALID_INCIDENT_TYPE(HttpStatus.BAD_REQUEST, 2101, "지원하지 않는 사고 타입입니다."),
    MISSING_LOCATION(HttpStatus.BAD_REQUEST, 2102, "위치 정보는 필수입니다."),
    MISSING_TIMESTAMP(HttpStatus.BAD_REQUEST, 2103, "타임스탬프는 필수입니다."),
    MISSING_ACCIDENT_ID(HttpStatus.BAD_REQUEST, 2104, "사고 이벤트에는 사고 ID가 필수입니다."),
    INVALID_OBSTACLE_DATA(HttpStatus.BAD_REQUEST, 2105, "장애물 이벤트에는 사고 ID가 있으면 안됩니다."),
    INVALID_LOCATION_BOUNDS(HttpStatus.BAD_REQUEST, 2106, "위치 좌표가 유효한 범위를 벗어났습니다."),
    INVALID_TIMESTAMP_FORMAT(HttpStatus.BAD_REQUEST, 2107, "타임스탬프 형식이 올바르지 않습니다."),

    // 2150번대: 사고 처리 에러
    INCIDENT_EVENT_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 2151, "사고 이벤트 처리 중 오류가 발생했습니다."),
    INCIDENT_MESSAGE_MAPPING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 2152, "사고 메시지 매핑 중 오류가 발생했습니다."),
    INCIDENT_NOTIFICATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 2153, "사고 알림 전송 중 오류가 발생했습니다."),
    VICINITY_SEARCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 2154, "주변 사용자 검색 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final Integer code;
    private final String message;
}
