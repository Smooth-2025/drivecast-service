package com.smooth.drivecast_service.driving.service;

import com.smooth.drivecast_service.driving.constants.DrivingVicinityPolicy;
import com.smooth.drivecast_service.global.common.cache.PresenceService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DrivingSessionManager {

    private final StringRedisTemplate stringRedisTemplate;
    private final PresenceService presenceService;

    public DrivingSessionManager(@Qualifier("stringRedisTemplate") StringRedisTemplate stringRedisTemplate,
                                PresenceService presenceService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.presenceService = presenceService;
    }

    public void addActiveUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }

        try {
            stringRedisTemplate.opsForSet().add(DrivingVicinityPolicy.DRIVING_ACTIVE_SET, userId);
            stringRedisTemplate.expire(DrivingVicinityPolicy.DRIVING_ACTIVE_SET, 
                    DrivingVicinityPolicy.ACTIVE_SET_TTL_SEC, TimeUnit.SECONDS);

            presenceService.markSeen(userId, Instant.now());
            
            log.debug("활성 세트 추가: userId={}", userId);
        } catch (Exception e) {
            log.warn("활성 세트 추가 실패: userId={}, 오류={}", userId, e.getMessage());
        }
    }

    public void removeActiveUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }

        try {
            stringRedisTemplate.opsForSet().remove(DrivingVicinityPolicy.DRIVING_ACTIVE_SET, userId);
            log.debug("활성 세트 제거: userId={}", userId);
        } catch (Exception e) {
            log.warn("활성 세트 제거 실패: userId={}, 오류={}", userId, e.getMessage());
        }
    }

    public Set<String> getActiveUsers() {
        try {
            var activeUsers = stringRedisTemplate.opsForSet().members(DrivingVicinityPolicy.DRIVING_ACTIVE_SET);
            return activeUsers != null ? activeUsers : Set.of();
        } catch (Exception e) {
            log.warn("활성 세트 조회 실패: {}", e.getMessage());
            return Set.of();
        }
    }
}