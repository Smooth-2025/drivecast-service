package com.smooth.drivecast_service.emergency.feign.dto;

import java.time.LocalDateTime;

public record EmergencyReportDto(
        Long id,
        String accidentId,
        Long userId,
        Boolean emergencyNotified,
        Boolean familyNotified,
        LocalDateTime reportTime,
        Double latitude,
        Double longitude
) {
}