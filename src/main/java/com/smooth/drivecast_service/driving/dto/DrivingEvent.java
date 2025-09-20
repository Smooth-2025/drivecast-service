package com.smooth.drivecast_service.driving.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.smooth.drivecast_service.driving.exception.DrivingErrorCode;
import com.smooth.drivecast_service.global.exception.BusinessException;
import com.smooth.drivecast_service.global.util.ValidationUtil;

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
