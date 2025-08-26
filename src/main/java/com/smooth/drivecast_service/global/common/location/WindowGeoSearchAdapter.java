package com.smooth.drivecast_service.global.common.location;

import com.smooth.drivecast_service.driving.dto.DrivingCoordinate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.geo.Distance;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 윈도우 기반 지리 검색 어댑터:
 * 순수 redis 지리 검색 로직만 포함*/
@Slf4j
@Component
@RequiredArgsConstructor
public class WindowGeoSearchAdapter {

    @Qualifier("valkeyRedisTemplate")
    private final RedisTemplate<String, String> valkeyRedisTemplate;

    /**
     * 여러 키에서 반경 검색 후 합집합 반환
     **/
    public Set<String> searchAcrossKeys(List<String> locationKeys, DrivingCoordinate center, int radiusMeters) {
        if (locationKeys == null || locationKeys.isEmpty() || center == null) {
            return Set.of();
        }

        var allUsers = new HashSet<String>();

        for (String key : locationKeys) {
            try {
                var usersInKey = searchInKey(key, center, radiusMeters);
                allUsers.addAll(usersInKey);

                log.debug("키별 지리 검색 완료: key={}, 발견={}명", key, usersInKey.size());
            } catch (Exception e) {
                log.warn("키별 지리 검색 실패: key={}, 오류={}", key, e.getMessage());
            }
        }

        log.debug("다중 키 지리 검색 완료: 검색키={}개, 중복제거={}명", locationKeys.size(), allUsers.size());
        return allUsers;
    }

    private Set<String> searchInKey(String locationKey, DrivingCoordinate center, int radiusMeters) {
        Set<String> users = new HashSet<>();

        try {
            var results = valkeyRedisTemplate.opsForGeo().search(
                    locationKey,
                    GeoReference.fromCoordinate(center.longitude(), center.latitude()),
                    new Distance(radiusMeters, Metrics.METERS),
                    RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance()
            );

            if (results != null && results.getContent() != null) {
                results.getContent().forEach(result -> {
                    if (result.getContent() != null && result.getContent().getName() != null) {
                        users.add(result.getContent().getName());
                    }
                });
            }

        } catch (Exception e) {
            log.warn("단일 키 지리 검색 오류: key={}, 오류={}", locationKey, e.getMessage());
        }

        return users;
    }
}
