package com.smooth.drivecast_service.driving.service;

import com.smooth.drivecast_service.driving.dto.DrivingCoordinate;
import com.smooth.drivecast_service.driving.util.LocationWindowKeyGenerator;
import com.smooth.drivecast_service.global.common.location.WindowLocationAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 주행 좌표 조회 서비스
 * 사용자들의 현재 위치 정보 제공
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class DrivingCoordinateService {

    private final WindowLocationAdapter windowLocationAdapter;

    /**
     * 단일 사용자 좌표 조회
     **/
    public Optional<DrivingCoordinate> getUserCoordinate(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }

        try {
            var windowKeys = LocationWindowKeyGenerator.generateDefaultWindowKeys(Instant.now());
            var location = windowLocationAdapter.findUserLocation(windowKeys, userId);
            
            if (location.isPresent()) {
                var coord = location.get();
                log.debug("사용자 좌표 조회 성공: userId={}, lat={}, lng={}", 
                        userId, coord.latitude(), coord.longitude());
                return Optional.of(coord);
            }
            
            log.debug("사용자 좌표 없음: userId={}", userId);
            return Optional.empty();
            
        } catch (Exception e) {
            log.warn("사용자 좌표 조회 실패: userId={}, 오류={}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 여러 사용자 좌표 일괄 조회
     **/
    public Map<String, DrivingCoordinate> getUserCoordinates(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        var result = new HashMap<String, DrivingCoordinate>();
        var windowKeys = LocationWindowKeyGenerator.generateDefaultWindowKeys(Instant.now());

        for (String userId : userIds) {
            try {
                var location = windowLocationAdapter.findUserLocation(windowKeys, userId);
                if (location.isPresent()) {
                    result.put(userId, location.get());
                }
            } catch (Exception e) {
                log.warn("사용자 좌표 조회 실패: userId={}, 오류={}", userId, e.getMessage());
            }
        }

        log.debug("좌표 일괄 조회 완료: 요청={}명, 조회={}명", userIds.size(), result.size());
        return result;
    }

    /**
     * 사용자 위치 존재 여부 확인
     **/
    public boolean hasUserLocation(String userId) {
        return getUserCoordinate(userId).isPresent();
    }

    /**
     * 여러 사용자 위치 존재 여부 확인
     **/
    public Map<String, Boolean> checkUserLocations(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        var result = new HashMap<String, Boolean>();
        var windowKeys = LocationWindowKeyGenerator.generateDefaultWindowKeys(Instant.now());

        for (String userId : userIds) {
            try {
                var hasLocation = windowLocationAdapter.findUserLocation(windowKeys, userId).isPresent();
                result.put(userId, hasLocation);
            } catch (Exception e) {
                log.warn("사용자 위치 확인 실패: userId={}, 오류={}", userId, e.getMessage());
                result.put(userId, false);
            }
        }

        return result;
    }
}