package com.smooth.drivecast_service.global.common.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * 사용자 접속 상태 관리 서비스 인터페이스
 * 정책이 없는 순수 저장/조회 기능만 제공
 **/
public interface PresenceService {

    /**
     * 사용자 마지막 접속 시각 기록
     * @param userId 사용자 ID
     * @param when 접속 시각
     **/
    void markSeen(String userId, Instant when);

    /**
     * 사용자 마지막 접속 시각 조회
     * @param userId 사용자 ID
     * @return 마지막 접속 시각 (없으면 empty)
     **/
    Optional<Instant> getLastSeen(String userId);

    /**
     * 사용자가 특정 시간 범위 내에 활동했는지 확인
     * @param userId 사용자 ID
     * @param refTime 기준 시각
     * @param skew 허용 오차
     * @return 활동 중이면 true
     **/
    boolean isFresh(String userId, Instant refTime, Duration skew);
}
