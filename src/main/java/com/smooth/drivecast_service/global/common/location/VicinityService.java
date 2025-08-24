package com.smooth.drivecast_service.global.common.location;

import java.time.Instant;
import java.util.List;

/**
 * 반경 내 사용자 탐색 서비스 인터페이스
 * 정책 파라미터는 도메인에서 결정하고, 구현체는 정책 없는 순수 어댑터
 **/
public interface VicinityService {

    /**
     * 반경 내 활성 사용자 조회
     * @param latitude 중심점 위도
     * @param longitude 중심점 경도
     * @param radiusMeters 반경(미터)
     * @param includeSelf 본인 포함 여부
     * @param freshnessSec 신선도 기준(초) - 이 시간 내 활동한 사용자만
     * @param maxRetries 최대 재시도 횟수
     * @param retryDelaysMs 재시도 지연 시간 목록
     * @param refTime 기준 시각
     * @param excludeUserId 제외할 사용자 ID (null 가능)
     * @return 조건에 맞는 사용자 ID 목록
     **/
    List<String> findUsers(double latitude, double longitude, int radiusMeters, boolean includeSelf, int freshnessSec, int maxRetries, List<Long> retryDelaysMs, Instant refTime, String excludeUserId);

    /**
     * 반경 내 모든 사용자 조회 (excludeUserId 없이)
     * OBSTACLE 이벤트처럼 신고자가 없는 경우 사용
     **/
    default List<String> findAllUsers(double latitude, double longitude, int radiusMeters, int freshnessSec, int maxRetries, List<Long> retryDelaysMs, Instant refTime) {
        return findUsers(latitude, longitude, radiusMeters, true, freshnessSec, maxRetries, retryDelaysMs, refTime, null);
    }
}
