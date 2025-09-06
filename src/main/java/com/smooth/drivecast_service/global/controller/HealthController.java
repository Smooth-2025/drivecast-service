package com.smooth.drivecast_service.global.controller;

import com.smooth.drivecast_service.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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
        healthInfo.put("version", "1.0.0");
        
        return ApiResponse.success("서비스 정상 동작 중", healthInfo);
    }

    @GetMapping("/readiness")
    public ApiResponse<Map<String, Object>> readiness() {
        Map<String, Object> readinessInfo = new HashMap<>();
        boolean isReady = true;
        
        // 메시징 Redis 연결 확인
        try {
            messagingRedisTemplate.opsForValue().set("health:check", "ok");
            messagingRedisTemplate.delete("health:check");
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

    @GetMapping("/connections")
    public ApiResponse<Map<String, Object>> getConnectionStats() {
        try {
            Map<String, Object> connectionStats = new HashMap<>();

            connectionStats.put("podId", System.getenv("HOSTNAME"));
            connectionStats.put("timestamp", LocalDateTime.now());
            connectionStats.put("note", "streams 도입을 위한 통계");
            
            return ApiResponse.success("연결 통계 조회 완료", connectionStats);
        } catch (Exception e) {
            log.error("연결 통계 조회 실패", e);
            return ApiResponse.success("연결 통계 조회 실패: " + e.getMessage(), null);
        }
    }
}