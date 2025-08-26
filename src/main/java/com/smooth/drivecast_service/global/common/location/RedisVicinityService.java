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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisVicinityService implements VicinityService {

    private final VicinityUserFinder vicinityUserFinder;
    @Qualifier("valkeyRedisTemplate")
    private final RedisTemplate<String, String> valkeyRedisTemplate;

    @Override
    public List<String> findUsers(double latitude,
                                  double longitude,
                                  int radiusMeters,
                                  boolean includeSelf,
                                  int freshnessSec,
                                  int maxRetries,
                                  List<Long> retryDelaysMs,
                                  Instant refTime,
                                  String excludeUserId) {

        // includeSelf가 false면 excludeUserId 설정
        String actualExcludeUserId = includeSelf ? null : excludeUserId;
        Duration freshness = Duration.ofSeconds(freshnessSec);

        return performWithRetry(
                latitude, longitude, radiusMeters, refTime, freshness, actualExcludeUserId, maxRetries, retryDelaysMs
        );
    }

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

    private List<String> performWithRetry(double latitude, double longitude, int radiusMeters, Instant refTime, Duration freshness, String excludeUserId, int maxRetries, List<Long> retryDelaysMs) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            log.debug("반경 내 사용자 검색 시도: {}차/{}", attempt, maxRetries);

            if (attempt > 1 && retryDelaysMs.size() >= attempt - 1) {
                sleep(retryDelaysMs.get(attempt - 2));
            }

            List<String> users = vicinityUserFinder.findUsersAround(
                    latitude, longitude, radiusMeters, refTime, freshness, excludeUserId
            );

            if (!users.isEmpty()) {
                log.debug("반경 내 사용자 검색 성공: {}차 시도, {}명 발견", attempt, users.size());
                return users;
            }
        }

        log.debug("반경 내 사용자 검색 실패: 최대 {}회 시도 완료", maxRetries);
        return List.of();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("재시도 지연 중 인터럽트 발생");
        }
    }
}
