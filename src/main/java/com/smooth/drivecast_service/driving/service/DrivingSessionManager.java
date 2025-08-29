package com.smooth.drivecast_service.driving.service;

import com.smooth.drivecast_service.driving.constants.DrivingVicinityPolicy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 주행 활성 세션 관리 서비스
 * 브로드캐스트 대상 사용자 세트 관리
 **/
@Slf4j
@Service
public class DrivingSessionManager {

    private final StringRedisTemplate stringRedisTemplate;

    public DrivingSessionManager(@Qualifier("stringRedisTemplate") StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 활성 세트에 사용자 추가
     **/
    public void addActiveUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }

        try {
            stringRedisTemplate.opsForSet().add(DrivingVicinityPolicy.DRIVING_ACTIVE_SET, userId);
            stringRedisTemplate.expire(DrivingVicinityPolicy.DRIVING_ACTIVE_SET, 
                    DrivingVicinityPolicy.ACTIVE_SET_TTL_SEC, TimeUnit.SECONDS);
            
            log.debug("활성 세트 추가: userId={}", userId);
        } catch (Exception e) {
            log.warn("활성 세트 추가 실패: userId={}, 오류={}", userId, e.getMessage());
        }
    }

    /**
     * 활성 세트에서 사용자 제거
     **/
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

    /**
     * 모든 활성 사용자 조회
     **/
    public Set<String> getActiveUsers() {
        try {
            var activeUsers = stringRedisTemplate.opsForSet().members(DrivingVicinityPolicy.DRIVING_ACTIVE_SET);
            return activeUsers != null ? activeUsers : Set.of();
        } catch (Exception e) {
            log.warn("활성 세트 조회 실패: {}", e.getMessage());
            return Set.of();
        }
    }

    /**
     * 활성 사용자 수 조회
     **/
    public long getActiveUserCount() {
        try {
            var count = stringRedisTemplate.opsForSet().size(DrivingVicinityPolicy.DRIVING_ACTIVE_SET);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.warn("활성 세트 크기 조회 실패: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 사용자가 활성 상태인지 확인
     **/
    public boolean isActiveUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }

        try {
            var isActive = stringRedisTemplate.opsForSet().isMember(DrivingVicinityPolicy.DRIVING_ACTIVE_SET, userId);
            return isActive != null && isActive;
        } catch (Exception e) {
            log.warn("활성 상태 확인 실패: userId={}, 오류={}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * 활성 세트 초기화 (운영/테스트용)
     **/
    public void clearActiveUsers() {
        try {
            stringRedisTemplate.delete(DrivingVicinityPolicy.DRIVING_ACTIVE_SET);
            log.info("활성 세트 초기화 완료");
        } catch (Exception e) {
            log.warn("활성 세트 초기화 실패: {}", e.getMessage());
        }
    }
}