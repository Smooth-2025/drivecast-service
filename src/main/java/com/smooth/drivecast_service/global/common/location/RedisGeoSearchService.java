package com.smooth.drivecast_service.global.common.location;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.geo.Distance;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisGeoSearchService implements GeoSearchService {

    @Qualifier("valkeyRedisTemplate")
    private final RedisTemplate<String, String> valkeyRedisTemplate;

    @Override
    public List<String> searchNearby(String locationKey, double latitude, double longitude, int radiusMeters) {
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

    @Override
    public boolean existsLocationKey(String locationKey) {
        try {
            return valkeyRedisTemplate.hasKey(locationKey);
        } catch (Exception e) {
            log.error("위치 키 존재 확인 실패: locationKey={}", locationKey, e);
            return false;
        }
    }
}
