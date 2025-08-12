package com.smooth.alert_service.core;

import com.smooth.alert_service.model.EventType;
import com.smooth.alert_service.model.AlertEvent;
import com.smooth.alert_service.support.util.LastSeenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.*;
import org.springframework.data.geo.Distance;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class VicinityUserFinder {

    private static final String GEO_KEY = "location:current";
    private final LastSeenService lastSeenService;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 반경 내 사용자 조회 (refTime ± skew 안에 lastseen이 존재하는 사용자만)
     * @param latitude  사고 지점 위도
     * @param longitude 사고 지점 경도
     * @param radiusMeters 반경(m)
     * @param refTime 기준 시각(최초: event.timestamp, 반복: Instant.now())
     * @param skew 허용 스큐(권장 3~7초)
     * @param excludeUserId 본인 제외용
     */
    public List<String> findUsersAround(double latitude,
                                        double longitude,
                                        int radiusMeters,
                                        Instant refTime,
                                        Duration skew,
                                        String excludeUserId) {

        // 좌표 유효성
        if (!isValidLatLng(latitude, longitude) || radiusMeters <= 0) {
            return List.of();
        }

        var geoOps = redisTemplate.opsForGeo();

        var results = geoOps.search(
                GEO_KEY,
                GeoReference.fromCoordinate(longitude, latitude),
                new Distance(radiusMeters, Metrics.METERS),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance()
        );

        if (results == null || results.getContent().isEmpty()) {
            return List.of();
        }

        return results.getContent().stream()
                .map(r -> r.getContent().getName()) // userId
                .filter(Objects::nonNull)
                .filter(uid -> excludeUserId == null || !uid.equals(excludeUserId))
                .filter(uid -> lastSeenService.isFresh(uid, refTime, skew))
                .toList();
    }

    public List<String> findUsersAroundByEventTime(double latitude,
                                                   double longitude,
                                                   int radiusMeters,
                                                   String eventTimestampIsoUtc,
                                                   Duration skew,
                                                   String excludeUserId) {
        try {
            Instant ref = Instant.parse(eventTimestampIsoUtc);
            return findUsersAround(latitude, longitude, radiusMeters, ref, skew, excludeUserId);
        } catch (Exception e) {
            // 파싱 실패 시 빈 결과
            return List.of();
        }
    }

    private boolean isValidLatLng(Double lat, Double lng) {
        if (lat == null || lng == null) return false;
        return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }
}
