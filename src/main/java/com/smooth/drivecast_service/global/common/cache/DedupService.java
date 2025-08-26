package com.smooth.drivecast_service.global.common.cache;

import java.time.Duration;

/**
 * 중복 방지 서비스 인터페이스
 * TTL 기반으로 중복 처리를 억제
 **/
public interface DedupService {

    /**
     * 첫 번째 요청인지 확인하고 마킹
     * @param key 중복 방지 키
     * @param ttl 키 유효 시간
     * @return 첫 번째 요청하면 true, 중복이면 false
     **/
    boolean markIfFirst(String key, Duration ttl);

    /**
     * 기본 TTL(3분)로 첫 번째 요청인지 확인
     * @param key 중복 방지 키
     * @return 첫 번째 요청이면 true, 중복이면 false
     **/
    default boolean markIfFirst(String key) {
        return markIfFirst(key, Duration.ofMinutes(3));
    }

    /**
     * 이미 처리된 요청인지 확인
     * @param key 중복 방지 키
     * @return 이미 처리되었으면 true
     **/
    boolean isAlreadyProcessed(String key);

    /**
     * 알림 중복 전송 방지 (AlertCacheService 기능 통합)
     * @param alertId 알림 ID
     * @param userId 사용자 ID
     * @return 첫 번째 전송이면 true, 중복이면 false
     **/
    default boolean markAlertIfFirst(String alertId, String userId) {
        String key = "alert:" + alertId + ":" + userId;
        return markIfFirst(key, Duration.ofMinutes(3));
    }

    /**
     * 알림이 이미 전송되었는지 확인
     * @param alertId 알림 ID
     * @param userId 사용자 ID
     * @return 이미 전송되었으면 true
     **/
    default boolean isAlertAlreadySent(String alertId, String userId) {
        String key = "alert:" + alertId + ":" + userId;
        return isAlreadyProcessed(key);
    }
}
