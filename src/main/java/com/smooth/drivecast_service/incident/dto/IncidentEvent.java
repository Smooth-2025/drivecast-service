package com.smooth.drivecast_service.incident.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.smooth.drivecast_service.global.exception.BusinessException;
import com.smooth.drivecast_service.global.util.ValidationUtil;
import com.smooth.drivecast_service.incident.exception.IncidentErrorCode;

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

        if (!ValidationUtil.hasValidIncidentTimestampFormat(timestamp)) {
            throw new BusinessException(IncidentErrorCode.INVALID_TIMESTAMP_FORMAT);
        }

        if (type == IncidentType.ACCIDENT && (accidentId == null || accidentId.isBlank())) {
            throw new BusinessException(IncidentErrorCode.MISSING_ACCIDENT_ID);
        }
        if (type == IncidentType.OBSTACLE && accidentId != null) {
            throw new BusinessException(IncidentErrorCode.INVALID_OBSTACLE_DATA);
        }
    }
}
