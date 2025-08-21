package com.smooth.drivecast_service.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 알림 중복 전송 방지용 캐시
 * - 키 규약: alert:{alertId}:{userId}
 * - TTL: 3분 (반복 알림 윈도우와 동일)
 *
 * 권장 사용:
 *   if (alertCacheService.markIfFirst(alertId, userId)) {
 *       // 최초 전송만 수행
 *       alertSender.sendToUser(userId, dto);
 *   }
 */
@Slf4j
@Component
public class AlertCacheService {

    private static final Duration TTL = Duration.ofMinutes(3);
    private final RedisTemplate<String, String> redisTemplate;

    public AlertCacheService(@Qualifier("stringRedisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String buildKey(String alertId, String userId) {
        return "alert:" + alertId + ":" + userId;
    }

    public boolean markIfFirst(String alertId, String userId) {
        try {
            String key = buildKey(alertId, userId);
            Boolean first = redisTemplate.opsForValue().setIfAbsent(key, "sent", TTL);
            boolean ok = Boolean.TRUE.equals(first);
            if (ok) {
                log.debug("알림 전송 최초 처리: key={}", key);
            } else {
                log.trace("알림 전송 이미 처리됨: key={}", key);
            }
            return ok;
        } catch (Exception e) {
            log.error("Redis 저장 실패(markIfFirst): alertId={}, userId={}", alertId, userId, e);
            return false;
        }
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
}
