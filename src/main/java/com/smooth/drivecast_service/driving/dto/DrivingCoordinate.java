package com.smooth.drivecast_service.driving.dto;

import com.smooth.drivecast_service.driving.exception.DrivingErrorCode;
import com.smooth.drivecast_service.global.exception.BusinessException;

/**
 * 지리적 좌표 DTO
 **/
public record DrivingCoordinate(double latitude, double longitude) {

    /**
     * 유효한 좌표인지 검증하는 생성자
     **/
    public DrivingCoordinate {
        if (!isValidLatitude(latitude)) {
            throw new BusinessException(DrivingErrorCode.INVALID_DRIVING_TYPE, "위도는 -90.0 ~ 90.0 범위여야 합니다: " + latitude);
        }
        if (!isValidLongitude(longitude)) {
            throw new BusinessException(DrivingErrorCode.INVALID_DRIVING_TYPE, "경도는 -180.0 ~ 180.0 범위여야 합니다: " + longitude);
        }
    }

    /**
     * 유효한 위도 범위 검증
     **/
    private static boolean isValidLatitude(double latitude) {
        return latitude >= -90.0 && latitude <= 90.0;
    }

    /**
     * 유효한 경도 범위 검증
     **/
    private static boolean isValidLongitude(double longitude) {
        return longitude >= -180.0 && longitude <= 180.0;
    }

    /**
     * 포맷된 문자열 표현
     **/
    @Override
    public String toString() {
        return "좌표{위도=%.6f, 경도=%.6f}".formatted(latitude, longitude);
    }
}
