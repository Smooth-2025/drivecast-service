package com.smooth.drivecast_service.emergency.dto;

import java.time.LocalDateTime;

public record EmergencyResponseDto(
    Boolean emergencyNotified,
    Boolean familyNotified,
    LocalDateTime reportedAt
) {}