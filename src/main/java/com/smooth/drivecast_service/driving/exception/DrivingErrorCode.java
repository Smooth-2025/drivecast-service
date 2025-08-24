package com.smooth.drivecast_service.driving.exception;

import com.smooth.drivecast_service.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum DrivingErrorCode implements ErrorCode {
    // 2000번대: 주행 도메인 에러
    INVALID_DRIVING_TYPE(HttpStatus.BAD_REQUEST, 2001, "지원하지 않는 주행 타입입니다."),
    MISSING_USER_ID(HttpStatus.BAD_REQUEST, 2002, "사용자 ID는 필수입니다."),
    MISSING_TIMESTAMP(HttpStatus.BAD_REQUEST, 2003, "타임스탬프는 필수입니다."),
    INVALID_TIMESTAMP_FORMAT(HttpStatus.BAD_REQUEST, 2004, "타임스탬프 형식이 올바르지 않습니다."),
    INVALID_USER_ID_FORMAT(HttpStatus.BAD_REQUEST, 2005, "사용자 ID 형식이 올바르지 않습니다."),

    // 2050번대: 주행 처리 에러
    DRIVING_EVENT_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 2051, "주행 이벤트 처리 중 오류가 발생했습니다."),
    DRIVING_MESSAGE_MAPPING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 2052, "주행 메시지 매핑 중 오류가 발생했습니다."),
    DRIVING_NOTIFICATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 2053, "주행 알림 전송 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final Integer code;
    private final String message;
}
