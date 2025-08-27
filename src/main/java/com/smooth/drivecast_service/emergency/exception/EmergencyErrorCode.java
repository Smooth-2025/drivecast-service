package com.smooth.drivecast_service.emergency.exception;

import com.smooth.drivecast_service.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum EmergencyErrorCode implements ErrorCode {

    // 2201-2299
    
    EMERGENCY_REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, 2201, "응급신고 기록을 찾을 수 없습니다."),
    EMERGENCY_REPORT_ALREADY_EXISTS(HttpStatus.CONFLICT, 2202, "이미 신고된 사고입니다."),
    EMERGENCY_ACCIDENT_INFO_NOT_FOUND(HttpStatus.NOT_FOUND, 2203, "사고 정보를 찾을 수 없습니다."),
    EMERGENCY_USER_INFO_NOT_FOUND(HttpStatus.NOT_FOUND, 2204, "사용자 정보를 찾을 수 없습니다."),
    EMERGENCY_INVALID_REQUEST_DATA(HttpStatus.BAD_REQUEST, 2205, "응급신고 요청 데이터가 유효하지 않습니다."),
    EMERGENCY_PHONE_NUMBER_CONVERSION_FAILED(HttpStatus.BAD_REQUEST, 2206, "전화번호 변환에 실패했습니다."),
    
    EMERGENCY_REPORT_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 2251, "응급신고 저장에 실패했습니다."),
    EMERGENCY_REPORT_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 2252, "응급신고 처리에 실패했습니다."),
    
    EMERGENCY_SMS_119_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 2261, "119 SMS 발송에 실패했습니다."),
    EMERGENCY_SMS_FAMILY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 2262, "가족 SMS 발송에 실패했습니다."),
    EMERGENCY_NOTIFICATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 2263, "응급 알림 발송에 실패했습니다."),
    
    EMERGENCY_USER_SERVICE_ERROR(HttpStatus.SERVICE_UNAVAILABLE, 2271, "사용자 서비스 조회에 실패했습니다."),
    EMERGENCY_TWILIO_SERVICE_ERROR(HttpStatus.SERVICE_UNAVAILABLE, 2272, "SMS 서비스 연동에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final Integer code;
    private final String message;
}