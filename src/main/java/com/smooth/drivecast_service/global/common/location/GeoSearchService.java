package com.smooth.drivecast_service.global.common.location;

import java.util.List;

/**
 * 지리(반경) 검색 서비스 인터페이스
 * 정책이 없는 순수한 지리(반경) 검색 기능만 제공
 **/
public interface GeoSearchService {

    /**
     * 특정 위치 주변의 사용자 검색
     * @param locationKey 위치 키 (예: location:20250822143000)
     * @param latitude 중심점 위도
     * @param longitude 중심점 경도
     * @param radiusMeters 검색 반경 (미터)
     * @return 반경 내 사용자 ID 목록
     **/
    List<String> searchNearby(String locationKey, double latitude, double longitude, int radiusMeters);

    /**
     * 위치 키 존재 여부 확인
     * @param locationKey 위치 키
     * @return 존재하면 true
     **/
    boolean existsLocationKey(String locationKey);
}
