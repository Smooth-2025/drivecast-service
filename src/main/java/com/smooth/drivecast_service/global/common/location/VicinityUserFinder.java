package com.smooth.drivecast_service.global.common.location;

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
import java.util.List;
import java.util.Objects;

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

        String locationKey = LocationKeyGenerator.generate(refTime);

        List<String> nearbyUsers = searchNearbyUsers(
                locationKey, latitude, longitude, radiusMeters
        );

        if (nearbyUsers.isEmpty()) {
            return List.of();
        }

        return nearbyUsers.stream()
                .filter(userId -> userId != null)
                .filter(userId -> excludeUserId == null || !userId.equals(excludeUserId))
                .filter(userId -> isUserActive(userId, refTime, freshness))
                .toList();
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
