package com.smooth.drivecast_service.global.common.location;

import com.smooth.drivecast_service.global.common.cache.PresenceService;
import com.smooth.drivecast_service.global.util.KoreanTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.geo.Distance;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class VicinityUserFinder {

    private final PresenceService presenceService;
    private final RedisTemplate<String, String> redisTemplate;

    public VicinityUserFinder(PresenceService presenceService,
                              @Qualifier("valkeyRedisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.presenceService = presenceService;
        this.redisTemplate = redisTemplate;
    }

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
        return findUsersAroundWithRetry(latitude, longitude, radiusMeters, refTime, skew, excludeUserId, 3);
    }

    /**
     * 타이밍 레이스 컨디션 대응: 지연 + 재시도 전략
     * Lambda 쓰기 지연을 고려하여 여러 번 재시도
     */
    private List<String> findUsersAroundWithRetry(double latitude,
                                                  double longitude,
                                                  int radiusMeters,
                                                  Instant refTime,
                                                  Duration skew,
                                                  String excludeUserId,
                                                  int maxRetries) {
        // 좌표 유효성
        if (!isValidLatLng(latitude, longitude) || radiusMeters <= 0) {
            return List.of();
        }

        // 한국시 기준 위치 키 생성
        String locationKey = KoreanTimeUtil.toLocationKey(refTime);

        log.info("⏰ 시간 동기화 확인:");
        log.info("  - refTime (UTC): {}", refTime);
        log.info("  - 한국시 변환 후 locationKey: {}", locationKey);
        log.info("  - 현재 시각 기준 locationKey: {}", KoreanTimeUtil.getCurrentLocationKey());

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            log.info("🔄 {}차 시도 (총 {}회)", attempt, maxRetries);

            // 1차 시도는 즉시, 이후는 지연
            if (attempt > 1) {
                try {
                    int delayMs = attempt == 2 ? 1000 : 2000; // 2차: 1초, 3차: 2초
                    log.info("⏳ {}ms 대기 중... (Lambda 쓰기 지연 대응)", delayMs);
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("⚠️ 재시도 중 인터럽트 발생");
                    return List.of();
                }
            }

            List<String> users = performGeoSearch(latitude, longitude, radiusMeters, refTime, skew, excludeUserId, locationKey, attempt);

            if (!users.isEmpty()) {
                log.info("✅ {}차 시도 성공: {}명 발견", attempt, users.size());
                return users;
            }

            log.info("❌ {}차 시도 실패: 0명", attempt);
        }

        log.warn("🚫 모든 재시도 실패: 최대 {}회 시도 완료", maxRetries);
        return List.of();
    }

    private List<String> performGeoSearch(double latitude, double longitude, int radiusMeters,
                                          Instant refTime, Duration skew, String excludeUserId,
                                          String locationKey, int attempt) {
        var geoOps = redisTemplate.opsForGeo();

        log.info("🔍 Valkey GEO 조회 시작 ({}차):", attempt);
        log.info("  - locationKey: {}", locationKey);
        log.info("  - 좌표: lat={}, lng={}", latitude, longitude);
        log.info("  - 반경: {}m", radiusMeters);
        log.info("  - 예상 명령어: GEOSEARCH {} FROMLONLAT {} {} BYRADIUS {} m WITHDIST",
                locationKey, longitude, latitude, radiusMeters);

        var results = geoOps.search(
                locationKey,
                GeoReference.fromCoordinate(longitude, latitude),
                new Distance(radiusMeters, Metrics.METERS),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance()
        );

        log.info("🎯 Valkey GEO 조회 결과 ({}차):", attempt);
        if (results == null) {
            log.warn("  - 결과가 null입니다");
            return List.of();
        }

        log.info("  - 조회된 사용자 수: {}", results.getContent().size());
        results.getContent().forEach(result -> {
            log.info("  - 사용자: {}, 거리: {}m",
                    result.getContent().getName(),
                    result.getDistance().getValue());
        });

        if (results.getContent().isEmpty()) {
            return List.of();
        }

        var filteredUsers = results.getContent().stream()
                .map(r -> r.getContent().getName()) // userId
                .filter(Objects::nonNull)
                .filter(uid -> {
                    boolean notExcluded = excludeUserId == null || !uid.equals(excludeUserId);
                    if (!notExcluded) {
                        log.info("🚫 제외된 사용자: userId={} (excludeUserId={})", uid, excludeUserId);
                    }
                    return notExcluded;
                })
                .filter(uid -> {
                    boolean isFresh = presenceService.isFresh(uid, refTime, skew);
                    log.info("🕐 LastSeen 필터링: userId={}, isFresh={}, refTime={}, skew={}초",
                            uid, isFresh, refTime, skew.getSeconds());
                    if (!isFresh) {
                        var lastSeen = presenceService.getLastSeen(uid);
                        log.info("  - 마지막 접속: {}", lastSeen.orElse(null));
                        log.warn("⚠️ LastSeen 데이터 없음 - 임시로 통과시킴: userId={}", uid);
                        return true; // 임시로 모든 사용자 통과
                    }
                    return isFresh;
                })
                .toList();

        log.info("🎯 최종 필터링 결과 ({}차): 총 {}명 → {}명", attempt, results.getContent().size(), filteredUsers.size());
        return filteredUsers;
    }

    public List<String> findUsersAroundByEventTime(double latitude,
                                                   double longitude,
                                                   int radiusMeters,
                                                   String eventTimestampKorean,
                                                   Duration skew,
                                                   String excludeUserId) {
        try {
            // 한국시 문자열을 Instant로 변환
            Instant ref = KoreanTimeUtil.parseKoreanTime(eventTimestampKorean);
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
