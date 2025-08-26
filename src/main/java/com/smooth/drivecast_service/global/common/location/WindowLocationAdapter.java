package com.smooth.drivecast_service.global.common.location;

import com.smooth.drivecast_service.driving.dto.DrivingCoordinate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 윈도우 기반 위치 조회 어댑터:
 * 순수 redis 위치 조회 로직만 포함
 **/
@Slf4j
@Component
@RequiredArgsConstructor
public class WindowLocationAdapter {

    @Qualifier("valkeyRedisTemplate")
    private final RedisTemplate<String, String> valkeyRedisTemplate;

    /**
     * 여러 키에서 사용자 위치 조회
     **/
    public Optional<DrivingCoordinate> findUserLocation(List<String> locationKeys, String userId) {
        if (locationKeys == null || locationKeys.isEmpty() || userId == null) {
            return Optional.empty();
        }

        for(String key : locationKeys) {
            try {
                var positions = valkeyRedisTemplate.opsForGeo().position(key, userId);

                if (positions != null && !positions.isEmpty() && positions.getFirst() != null) {
                    var point = positions.getFirst();
                    var coordinate = new DrivingCoordinate(point.getY(), point.getX());

                    log.debug("사용자 위치 발견: userId={}, key={}, 좌표={}", userId, key, coordinate);
                    return Optional.of(coordinate);
                }
            } catch (Exception e) {
                log.warn("사용자 위치 조회 실패: userId={}, key={}, 오류={}", userId, key, e.getMessage());
            }
        }

        log.debug("사용자 위치 없음: userId={}, 검색키={}개", userId, locationKeys.size());
        return Optional.empty();
    }
}
