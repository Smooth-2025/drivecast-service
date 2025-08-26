package com.smooth.drivecast_service.incident.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.smooth.drivecast_service.global.exception.BusinessException;
import com.smooth.drivecast_service.global.util.ValidationUtil;
import com.smooth.drivecast_service.incident.exception.IncidentErrorCode;

/**
 * 사고/장애물 도메인 전용 이벤트
 * - API 호환성을 위한 JsonAlias 지원
 * - 불변 객체로 데이터 무결성 보장
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public record IncidentEvent(
        @JsonAlias({"eventType", "type"}) IncidentType type,
        String accidentId,
        @JsonAlias({"vehicleId", "userId"}) String userId,
        Double latitude,
        Double longitude,
        String timestamp
) {
    public IncidentEvent {
        if (type == null) {
            throw new BusinessException(IncidentErrorCode.INVALID_INCIDENT_TYPE);
        }
        if (!ValidationUtil.isValidCoordinate(latitude,longitude)) {
            throw new BusinessException(IncidentErrorCode.INVALID_LOCATION_COORDINATES);
        }
        if (!ValidationUtil.isNotBlank(timestamp)) {
            throw new BusinessException(IncidentErrorCode.MISSING_TIMESTAMP);
        }

        // 위치 좌표 범위 검증
        if (!ValidationUtil.hasValidTimestampFormat(timestamp)) {
            throw new BusinessException(IncidentErrorCode.INVALID_TIMESTAMP_FORMAT);
        }

        // 타입별 특수 검증
        if (type == IncidentType.ACCIDENT && (accidentId == null || accidentId.isBlank())) {
            throw new BusinessException(IncidentErrorCode.MISSING_ACCIDENT_ID);
        }
        if (type == IncidentType.OBSTACLE && accidentId != null) {
            throw new BusinessException(IncidentErrorCode.INVALID_OBSTACLE_DATA);
        }
    }
}
