package com.smooth.drivecast_service.driving.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.smooth.drivecast_service.driving.exception.DrivingErrorCode;
import com.smooth.drivecast_service.global.exception.BusinessException;
import com.smooth.drivecast_service.global.util.ValidationUtil;

/**
 * 주행 도메인 전용 이벤트
 * - API 호환성을 위해 JsonAlias 지원
 * - 불변 객체로 데이터 무결성 보장
 **/
public record DrivingEvent(
        @JsonAlias({"eventType", "type"}) DrivingType type,
        @JsonAlias({"vehicleId", "userId"}) String userId,
        String timestamp
) {
    public DrivingEvent {
        if (type == null) {
            throw new BusinessException(DrivingErrorCode.INVALID_DRIVING_TYPE);
        }
        if (!ValidationUtil.isValidUserId(userId)) {
            throw new BusinessException(DrivingErrorCode.MISSING_USER_ID);
        }
        if (!ValidationUtil.isNotBlank(timestamp)) {
            throw new BusinessException(DrivingErrorCode.MISSING_TIMESTAMP);
        }
        if (!ValidationUtil.hasValidTimestampFormat(timestamp)) {
            throw new BusinessException(DrivingErrorCode.INVALID_TIMESTAMP_FORMAT);
        }
    }
}
