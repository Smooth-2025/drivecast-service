package com.smooth.drivecast_service.driving.service;

import com.smooth.drivecast_service.driving.constants.DrivingDestinations;
import com.smooth.drivecast_service.driving.dto.DrivingResponseDto;
import com.smooth.drivecast_service.global.common.notification.RealtimePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 주행 근접 운전자 성향 실시간 브로드캐스트 스케줄러
 * 1Hz 주기로 활성 사용자들에게 주변 운전자 성향 정보 전송
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class DrivingBroadcastScheduler {

    private final RealtimePublisher realtimePublisher;
    private final DrivingSessionManager sessionManager;
    private final DrivingVicinityService vicinityService;
    private final TraitCacheService traitCacheService;
    private final DrivingCoordinateService coordinateService;
    
    // 연속으로 활성 사용자가 없었던 횟수 추적
    private int consecutiveEmptyChecks = 0;
    private static final int MAX_EMPTY_CHECKS = 10; // 10초 후 체크 간격 늘리기

    /**
     * 1초마다 주변 운전자 성향 브로드캐스트
     **/
    @Scheduled(fixedRate = 1000) // 1Hz = 1000ms
    public void broadcastNearbyTraits() {
        var startTime = System.currentTimeMillis();
        
        try {
            var activeUsers = sessionManager.getActiveUsers();
            if (activeUsers.isEmpty()) {
                consecutiveEmptyChecks++;
                
                // 연속으로 활성 사용자가 없으면 로그 빈도 줄이기
                if (consecutiveEmptyChecks <= MAX_EMPTY_CHECKS) {
                    log.debug("활성 사용자 없음: 연속 {}회", consecutiveEmptyChecks);
                } else if (consecutiveEmptyChecks % 60 == 0) { // 1분마다 한번씩만 로그
                    log.debug("활성 사용자 없음: 연속 {}회 (1분마다 로그)", consecutiveEmptyChecks);
                }
                return; // 활성 사용자 없음
            }
            
            // 활성 사용자가 있으면 카운터 리셋
            if (consecutiveEmptyChecks > 0) {
                log.info("활성 사용자 발견: {}명 ({}초 만에 재개)", activeUsers.size(), consecutiveEmptyChecks);
                consecutiveEmptyChecks = 0;
            }

            var processedCount = 0;
            var totalNearbyCount = 0;

            for (String userId : activeUsers) {
                try {
                    var vicinityData = processUserVicinityData(userId);
                    if (vicinityData != null) {
                        sendVicinityDataToUser(userId, vicinityData);
                        processedCount++;
                        totalNearbyCount += vicinityData.neighbors().size();
                    }
                } catch (Exception e) {
                    log.warn("사용자 성향 브로드캐스트 실패: userId={}, 오류={}", userId, e.getMessage());
                }
            }

            var elapsed = System.currentTimeMillis() - startTime;
            if (processedCount > 0) {
                log.debug("성향 브로드캐스트 완료: 활성={}명, 처리={}명, 주변={}명, 소요={}ms", 
                        activeUsers.size(), processedCount, totalNearbyCount, elapsed);
            }

        } catch (Exception e) {
            log.error("성향 브로드캐스트 스케줄러 오류", e);
        }
    }

    /**
     * 사용자 주변 운전자 데이터 처리
     **/
    private VicinityData processUserVicinityData(String userId) {
        try {
            // 1. ego 위치 조회
            var egoCoordinate = coordinateService.getUserCoordinate(userId);
            if (egoCoordinate.isEmpty()) {
                log.debug("ego 위치 없음: userId={}", userId);
                return null;
            }

            // 2. 주변 운전자 탐지
            var nearbyDrivers = vicinityService.findNearbyDrivers(userId);
            if (nearbyDrivers.isEmpty()) {
                // 주변 운전자가 없어도 ego 정보는 전송
                return new VicinityData(
                    new EgoData(userId, egoCoordinate.get()),
                    List.of()
                );
            }

            // 3. 주변 운전자 좌표 조회
            var nearbyCoordinates = coordinateService.getUserCoordinates(nearbyDrivers);

            // 4. 성향 조회 (캐시 우선)
            var traits = traitCacheService.getTraitsForUsers(nearbyDrivers);

            // 5. 이웃 데이터 생성 (좌표와 성향이 모두 있는 경우만)
            var neighbors = nearbyDrivers.stream()
                    .filter(nearbyCoordinates::containsKey)
                    .filter(traits::containsKey)
                    .map(neighborId -> new NeighborData(
                            neighborId,
                            traits.get(neighborId),
                            nearbyCoordinates.get(neighborId)
                    ))
                    .toList();
            
            log.debug("사용자 주변 데이터 처리: ego={}, 주변={}명, 완성={}명", 
                    userId, nearbyDrivers.size(), neighbors.size());
            
            return new VicinityData(
                new EgoData(userId, egoCoordinate.get()),
                neighbors
            );

        } catch (Exception e) {
            log.warn("사용자 주변 데이터 처리 실패: userId={}, 오류={}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * 사용자에게 주변 데이터 전송
     **/
    private void sendVicinityDataToUser(String userId, VicinityData vicinityData) {
        try {
            // 기존 DrivingResponseDto 활용
            var payload = createVicinityPayload(vicinityData);
            var response = new DrivingResponseDto("driving", payload);
            
            // RealtimePublisher를 통해 전송 (기존 패턴과 동일)
            realtimePublisher.toUser(userId, DrivingDestinations.DRIVING_STATUS, response);
            
            log.debug("주변 데이터 전송 완료: userId={}, 이웃={}명", userId, vicinityData.neighbors().size());

        } catch (Exception e) {
            log.warn("주변 데이터 전송 실패: userId={}, 오류={}", userId, e.getMessage());
        }
    }

    /**
     * 주변 데이터 페이로드 생성
     **/
    private Map<String, Object> createVicinityPayload(VicinityData vicinityData) {
        var payload = new HashMap<String, Object>();
        
        // timestamp (ISO 8601 형식)
        payload.put("timestamp", java.time.Instant.now().toString());
        
        // ego 데이터
        var egoMap = Map.of(
                "userId", vicinityData.ego().userId(),
                "pose", Map.of(
                        "latitude", vicinityData.ego().coordinate().latitude(),
                        "longitude", vicinityData.ego().coordinate().longitude()
                )
        );
        payload.put("ego", egoMap);
        
        // neighbors 데이터
        var neighborsList = vicinityData.neighbors().stream()
                .map(neighbor -> Map.of(
                        "userId", neighbor.userId(),
                        "character", neighbor.character(),
                        "pose", Map.of(
                                "latitude", neighbor.coordinate().latitude(),
                                "longitude", neighbor.coordinate().longitude()
                        )
                ))
                .toList();
        payload.put("neighbors", neighborsList);
        
        return payload;
    }



    /**
     * 주변 데이터 전체 정보
     **/
    private record VicinityData(EgoData ego, java.util.List<NeighborData> neighbors) {}

    /**
     * Ego 사용자 데이터
     **/
    private record EgoData(String userId, com.smooth.drivecast_service.driving.dto.DrivingCoordinate coordinate) {}

    /**
     * 이웃 사용자 데이터
     **/
    private record NeighborData(String userId, String character, com.smooth.drivecast_service.driving.dto.DrivingCoordinate coordinate) {}
}