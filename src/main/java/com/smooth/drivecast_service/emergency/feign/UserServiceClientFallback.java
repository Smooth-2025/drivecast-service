package com.smooth.drivecast_service.emergency.feign;

import com.smooth.drivecast_service.emergency.feign.dto.EmergencyInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserServiceClientFallback implements UserServiceClient {
    @Override
    public EmergencyInfoResponse getUserInfo(String userId) {
        log.error("유저 서비스 장애 userId = {}", userId);

        EmergencyInfoResponse.EmergencyData fallbackData = EmergencyInfoResponse.EmergencyData.builder()
                .userId(userId)
                .gender("미상")
                .bloodType("미상")
                .emergencyContact1(null)
                .emergencyContact2(null)
                .emergencyContact3(null)
                .build();

        return EmergencyInfoResponse.builder()
                .code("SERVICE_UNAVAILABLE")
                .message("유저 서비스 장애로 인한 기본 정보")
                .data(fallbackData)
                .build();
    }
}
