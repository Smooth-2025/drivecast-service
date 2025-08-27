package com.smooth.drivecast_service.global.controller;

import com.smooth.drivecast_service.global.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> healthInfo = Map.of(
            "status", "UP",
            "service", "drivecast-service",
            "timestamp", LocalDateTime.now(),
            "version", "1.0.0"
        );
        
        return ApiResponse.success("서비스 정상 동작 중", healthInfo);
    }
}