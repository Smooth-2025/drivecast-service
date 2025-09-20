package com.smooth.drivecast_service.incident.service.mapper;

import com.smooth.drivecast_service.incident.dto.IncidentResponseDto;

import java.util.Optional;

public interface IncidentMessageMapper {

    boolean supports(String incidentType);

    Optional<IncidentResponseDto> map(IncidentMappingContext context);

    default Optional<IncidentResponseDto> map(IncidentMappingContext context, String targetUserId) {
        var contextWithUser = IncidentMappingContext.of(context.getEvent(), targetUserId);
        return map(contextWithUser);
    }
}
