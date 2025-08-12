package com.smooth.alert_service.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
public class AlertCacheService {

    private static final Duration TTL = Duration.ofMinutes(3);
    private final RedisTemplate<String, String> redisTemplate;

    public AlertCacheService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAlreadySent(String alertId, String userId) {
        try {
            String key = buildKey(alertId, userId);
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Redis 조회 실패: alertId={}, userId={}", alertId, userId, e);
            return false;
        }
    }

    public void markAsSent(String alertId, String userId) {
        try {
            String key = buildKey(alertId, userId);
            redisTemplate.opsForValue().set(key, "sent", TTL);
            log.debug("알림 전송 기록 저장: key={}", key);
        } catch (Exception e) {
            log.error("Redis 저장 실패: alertId={}, userId={}", alertId, userId, e);
        }
    }

    private String buildKey(String alertId, String userId) {
        return "alert:" + alertId + ":" + userId;
    }
}
