package com.smooth.drivecast_service.global.controller;

import com.smooth.drivecast_service.global.common.ApiResponse;
import com.smooth.drivecast_service.global.exception.CommonErrorCode;
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

    @GetMapping("/test-pubsub")
    public ApiResponse<Map<String, Object>> testPubSub() {
        try {
            String testChannel = "websocket:messages";
            String testMessage = "{\"userId\":\"test-user\",\"destination\":\"/topic/test\",\"payload\":{\"message\":\"Pub/Sub 테스트\",\"timestamp\":\"" + LocalDateTime.now() + "\"},\"sourcePodId\":\"" + System.getenv("HOSTNAME") + "\"}";
            
            // Redis Pub/Sub으로 테스트 메시지 발행
            messagingRedisTemplate.convertAndSend(testChannel, testMessage);
            
            Map<String, Object> result = new HashMap<>();
            result.put("channel", testChannel);
            result.put("message", testMessage);
            result.put("timestamp", LocalDateTime.now());
            result.put("podId", System.getenv("HOSTNAME"));
            
            log.info("Pub/Sub 테스트 메시지 발행: channel={}", testChannel);
            
            return ApiResponse.success("Pub/Sub 테스트 메시지 발행 완료", result);
        } catch (Exception e) {
            log.error("Pub/Sub 테스트 실패", e);
            return ApiResponse.error(CommonErrorCode.INTERNAL_SERVER_ERROR, "Pub/Sub 테스트 실패: " + e.getMessage());
        }
    }

    @GetMapping("/test-incident")
    public ApiResponse<Map<String, Object>> testIncidentAlert() {
        try {
            String testChannel = "websocket:messages";
            String testMessage = "{\"userId\":\"test-user\",\"destination\":\"/topic/incident\",\"payload\":{\"type\":\"accident-nearby\",\"title\":\"전방 사고 발생!\",\"content\":\"근처 차량에서 큰 사고가 발생했습니다. 안전 운전하세요.\"},\"sourcePodId\":\"" + System.getenv("HOSTNAME") + "\"}";
            
            // 사고 알림 테스트 메시지 발행
            messagingRedisTemplate.convertAndSend(testChannel, testMessage);
            
            Map<String, Object> result = new HashMap<>();
            result.put("channel", testChannel);
            result.put("message", testMessage);
            result.put("timestamp", LocalDateTime.now());
            result.put("podId", System.getenv("HOSTNAME"));
            
            log.info("사고 알림 테스트 메시지 발행: channel={}", testChannel);
            
            return ApiResponse.success("사고 알림 테스트 메시지 발행 완료", result);
        } catch (Exception e) {
            log.error("사고 알림 테스트 실패", e);
            return ApiResponse.error(CommonErrorCode.INTERNAL_SERVER_ERROR, "사고 알림 테스트 실패: " + e.getMessage());
        }
    }
}