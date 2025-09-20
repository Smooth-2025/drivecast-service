package com.smooth.drivecast_service.incident.dto;

import java.util.Map;

public record IncidentResponseDto(
        String type,
        Map<String, Object> payload
) {}