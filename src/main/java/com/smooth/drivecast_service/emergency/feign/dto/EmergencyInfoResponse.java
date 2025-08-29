package com.smooth.drivecast_service.emergency.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyInfoResponse {
    private String code;
    private String message;
    private EmergencyData data;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmergencyData {
        private String userId;
        private String gender;
        private String bloodType;
        private String emergencyContact1;
        private String emergencyContact2;
        private String emergencyContact3;
    }
}