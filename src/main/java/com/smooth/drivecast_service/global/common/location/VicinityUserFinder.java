package com.smooth.drivecast_service.global.common.location;

import com.smooth.drivecast_service.global.common.cache.PresenceService;
import com.smooth.drivecast_service.global.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VicinityUserFinder {

    private final VicinityService vicinityService;
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

        List<String> nearbyUsers = vicinityService.searchNearby(
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

    private boolean isUserActive(String userId, Instant refTime, Duration freshness) {
        try {
            return presenceService.isFresh(userId, refTime, freshness);
        } catch (Exception e) {
            log.warn("사용자 활성 상태 확인 실패: userId={}", userId, e);
            return false;
        }
    }
}
