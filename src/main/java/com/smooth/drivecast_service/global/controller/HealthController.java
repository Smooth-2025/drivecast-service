package com.smooth.drivecast_service.global.controller;

import com.smooth.drivecast_service.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.smooth.drivecast_service.global.constants.GlobalConstants.Redis.HEALTH_CHECK_KEY;
import static com.smooth.drivecast_service.global.constants.GlobalConstants.Redis.HEALTH_CHECK_TTL;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    @Qualifier("messagingStringRedisTemplate")
    private final StringRedisTemplate messagingRedisTemplate;

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("service", "drivecast-service");
        healthInfo.put("timestamp", LocalDateTime.now());
        
        return ApiResponse.success("서비스 정상 동작 중", healthInfo);
    }

    @GetMapping("/readiness")
    public ApiResponse<Map<String, Object>> readiness() {
        Map<String, Object> readinessInfo = new HashMap<>();
        boolean isReady = true;

        try {
            messagingRedisTemplate.opsForValue().set(HEALTH_CHECK_KEY, "ok", HEALTH_CHECK_TTL);
            readinessInfo.put("messagingRedis", "UP");
        } catch (Exception e) {
            log.error("메시징 Redis 연결 실패", e);
            readinessInfo.put("messagingRedis", "DOWN");
            isReady = false;
        }
        
        readinessInfo.put("status", isReady ? "READY" : "NOT_READY");
        readinessInfo.put("timestamp", LocalDateTime.now());
        
        return ApiResponse.success(isReady ? "서비스 준비 완료" : "서비스 준비 중", readinessInfo);
    }
}