package com.smooth.drivecast_service.driving.dto;

import java.util.Map;

public record DrivingResponseDto(
        String type,
        Map<String, Object> payload
) {}
