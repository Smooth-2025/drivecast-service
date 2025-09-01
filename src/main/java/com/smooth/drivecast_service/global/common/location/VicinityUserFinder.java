package com.smooth.drivecast_service.global.common.location;

import com.smooth.drivecast_service.driving.util.LocationWindowKeyGenerator;
import com.smooth.drivecast_service.global.common.cache.PresenceService;
import com.smooth.drivecast_service.global.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.geo.Distance;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class VicinityUserFinder {

    @Qualifier("valkeyRedisTemplate")
    private final RedisTemplate<String, String> valkeyRedisTemplate;
    private final PresenceService presenceService;

    public List<String> findUsersAround(double latitude,
                                        double longitude,
                                        int radiusMeters,
                                        Instant refTime,
                                        Duration freshness,
                                        String excludeUserId)  {
        if (!ValidationUtil.isValidCoordinate(latitude, longitude) || radiusMeters <= 0) {
            return List.of();
        }

        // 윈도우 키 방식 사용 (driving과 동일한 방식)
        List<String> windowKeys = LocationWindowKeyGenerator.generateDefaultWindowKeys(refTime);
        log.debug("윈도우 키 생성: {}", windowKeys);

        Set<String> allNearbyUsers = new HashSet<>();
        
        // 각 윈도우 키에서 사용자 검색
        for (String locationKey : windowKeys) {
            List<String> usersInWindow = searchNearbyUsers(locationKey, latitude, longitude, radiusMeters);
            allNearbyUsers.addAll(usersInWindow);
        }

        if (allNearbyUsers.isEmpty()) {
            log.debug("모든 윈도우에서 사용자 없음: windowKeys={}", windowKeys.size());
            return List.of();
        }

        // 필터링 및 신선도 체크
        List<String> result = new ArrayList<>();
        for (String userId : allNearbyUsers) {
            if (userId != null && 
                (excludeUserId == null || !userId.equals(excludeUserId)) &&
                isUserActive(userId, refTime, freshness)) {
                result.add(userId);
            }
        }

        log.debug("윈도우 기반 사용자 검색 완료: 윈도우={}개, 전체={}명, 필터링후={}명", 
                windowKeys.size(), allNearbyUsers.size(), result.size());
        
        return result;
    }

    private List<String> searchNearbyUsers(String locationKey, double latitude, double longitude, int radiusMeters) {
        try {
            var geoOps = valkeyRedisTemplate.opsForGeo();

            var results = geoOps.search(
                    locationKey,
                    GeoReference.fromCoordinate(longitude, latitude),
                    new Distance(radiusMeters, Metrics.METERS),
                    RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance()
            );

            if (results == null || results.getContent().isEmpty()) {
                log.debug("반경 내 사용자 없음: locationKey={}, lat={}, lng={}, radius={}m", locationKey, latitude, longitude, radiusMeters);
                return List.of();
            }

            List<String> userIds = results.getContent().stream()
                    .map(r -> r.getContent().getName())
                    .filter(Objects::nonNull)
                    .toList();

            log.debug("반경 내 사용자 검색 완료: locationKey={}, 발견={}명", locationKey, userIds.size());
            return userIds;
        } catch (Exception e) {
            log.error("지리적 검색 실패: locationKey={}, lat={}, lng={}, radius={}m", locationKey, latitude, longitude, radiusMeters, e);
            return List.of();
        }
    }

    private boolean isUserActive(String userId, Instant refTime, Duration freshness) {
        try {
            return presenceService.isFresh(userId, refTime, freshness);
        } catch (Exception e) {
            log.warn("사용자 활성 상태 확인 실패: userId={}", userId, e);
            return false;
        }
    }
}
