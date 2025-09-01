package com.smooth.drivecast_service.driving.service;

import com.smooth.drivecast_service.driving.constants.DrivingVicinityPolicy;
import java.util.ArrayList;
import com.smooth.drivecast_service.driving.util.LocationWindowKeyGenerator;
import com.smooth.drivecast_service.global.common.cache.PresenceService;
import com.smooth.drivecast_service.global.common.location.WindowGeoSearchAdapter;
import com.smooth.drivecast_service.global.common.location.WindowLocationAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 주행 근접 운전자 탐지 도메인 서비스: 도메인 정책을 포함하고 어댑터들을 조합
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class DrivingVicinityService {

    private final WindowLocationAdapter windowLocationAdapter;
    private final WindowGeoSearchAdapter windowGeoSearchAdapter;
    private final PresenceService presenceService;

    /**
     * 주변 운전자 탐지
     **/
    public List<String> findNearbyDrivers(String egoUserId) {
        return findNearbyDriversWithRetry(egoUserId, Instant.now());
    }

    private List<String> findNearbyDriversWithRetry(String egoUserId, Instant refTime) {
        for(int attempt = 1; attempt <= DrivingVicinityPolicy.MAX_RETRIES; attempt++) {

            if (attempt > 1) {
                sleep(DrivingVicinityPolicy.RETRY_DELAYS_MS[attempt - 2]);
            }

            var nearbyDrivers = findNearbyDriversOnce(egoUserId, refTime);

            if (!nearbyDrivers.isEmpty()) {
                log.debug("주변 운전자 탐지 성공: 시도={}/{}, 발견={}명", attempt, DrivingVicinityPolicy.MAX_RETRIES, nearbyDrivers.size());
                return nearbyDrivers;
            }
        }

        log.debug("주변 운전자 탐지 실패: 최대 {}회 시도 완료", DrivingVicinityPolicy.MAX_RETRIES);
        return List.of();
    }

    private List<String> findNearbyDriversOnce(String egoUserId, Instant refTime) {
        // 1. 윈도우 키 생성
        var windowKeys = LocationWindowKeyGenerator.generateDefaultWindowKeys(refTime);
        log.debug("생성된 윈도우 키들: {}", windowKeys);

        // 2. ego 위치 찾기
        var egoLocation = windowLocationAdapter.findUserLocation(windowKeys, egoUserId);
        if(egoLocation.isEmpty()) {
            log.debug("자차 위치 없음: userId={}, 검색키={}", egoUserId, windowKeys);
            return List.of();
        }

        // 3. 반경 검색
        var nearbyUsers = windowGeoSearchAdapter.searchAcrossKeys(
                windowKeys,
                egoLocation.get(),
                DrivingVicinityPolicy.RADIUS_METERS
        );

        // 4. 본인 제외
        nearbyUsers.remove(egoUserId);

        // 5. 신선도 필터링
        var freshUsers = new ArrayList<String>();
        for (String userId : nearbyUsers) {
            var lastSeen = presenceService.getLastSeen(userId);
            if (lastSeen.isPresent()) {
                var age = Duration.between(lastSeen.get(), refTime);
                boolean isFresh = age.compareTo(DrivingVicinityPolicy.FRESHNESS_THRESHOLD) <= 0;
                log.debug("신선도 체크: userId={}, lastSeen={}, age={}초, fresh={}", 
                         userId, lastSeen.get(), age.getSeconds(), isFresh);
                if (isFresh) {
                    freshUsers.add(userId);
                }
            } else {
                log.debug("신선도 체크: userId={}, lastSeen=없음", userId);
            }
        }
        
        log.debug("신선도 필터링: 반경내={}명, 신선={}명", nearbyUsers.size(), freshUsers.size());

        log.debug("근접 탐지 완료: ego={}, 윈도우={}개, 반경내={}명, 신선={}명",
                egoUserId, windowKeys.size(), nearbyUsers.size(), freshUsers.size());

        return freshUsers;
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("재시도 대기 중 인터럽트 발생");
        }
    }
}
