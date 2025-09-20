package com.smooth.drivecast_service.driving.service;

import com.smooth.drivecast_service.driving.constants.DrivingVicinityPolicy;
import com.smooth.drivecast_service.global.exception.BusinessException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TraitCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final DrivingTraitService drivingTraitService;

    public TraitCacheService(@Qualifier("stringRedisTemplate") RedisTemplate<String, String> redisTemplate,
                           DrivingTraitService drivingTraitService) {
        this.redisTemplate = redisTemplate;
        this.drivingTraitService = drivingTraitService;
    }

    @Scheduled(cron = "0 */10 * * * *", zone = "Asia/Seoul")
    public void warmupCache() {
        var startTime = System.currentTimeMillis();
        log.info("성향 워밍 캐시 시작");

        try {
            var traits = drivingTraitService.exportTraits();
            if (!traits.isEmpty()) {
                saveToWarmCache(traits);
                log.info("성향 워밍 캐시 완료: 저장={}명", traits.size());
            } else {
                log.warn("성향 워밍 캐시: 저장할 데이터 없음");
            }

            var elapsed = System.currentTimeMillis() - startTime;
            log.info("성향 워밍 캐시 소요시간: {}ms", elapsed);

        } catch (BusinessException e) {
            log.error("성향 워밍 캐시 비즈니스 오류: {}", e.getMessage());
        } catch (Exception e) {
            log.error("성향 워밍 캐시 실패", e);
        }
    }

    public Map<String, String> getTraitsForUsers(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        var startTime = System.currentTimeMillis();
        var result = new HashMap<String, String>();

        var hotResults = getFromHotCache(userIds);
        result.putAll(hotResults);

        var hotMissUsers = userIds.stream()
                .filter(userId -> !result.containsKey(userId))
                .toList();

        if (!hotMissUsers.isEmpty()) {
            var warmResults = getFromWarmCache(hotMissUsers);
            result.putAll(warmResults);

            if (!warmResults.isEmpty()) {
                saveToHotCache(warmResults);
            }
        }

        var apiMissUsers = userIds.stream()
                .filter(userId -> !result.containsKey(userId))
                .toList();

        if (!apiMissUsers.isEmpty()) {
            var apiResults = drivingTraitService.getTraitsFromApi(apiMissUsers);
            result.putAll(apiResults);

            if (!apiResults.isEmpty()) {
                saveToHotCache(apiResults);
            }

            log.debug("API 조회 완료: {}명", apiResults.size());
        }

        var elapsed = System.currentTimeMillis() - startTime;
        log.debug("성향 조회 완료: 요청={}명, 핫={}명, 워밍={}명, API={}명, 소요={}ms",
                userIds.size(), hotResults.size(),
                hotMissUsers.size() - apiMissUsers.size(), apiMissUsers.size(), elapsed);

        return result;
    }

    private Map<String, String> getFromHotCache(List<String> userIds) {
        var result = new HashMap<String, String>();

        try {
            var keys = userIds.stream()
                    .map(userId -> DrivingVicinityPolicy.TRAIT_HOT_PREFIX + userId)
                    .toList();

            var values = redisTemplate.opsForValue().multiGet(keys);

            for (int i = 0; i < userIds.size(); i++) {
                var value = values.get(i);
                if (value != null) {
                    var character = parseCharacter(value);
                    if (character != null) {
                        result.put(userIds.get(i), character);
                    }
                }
            }

            log.debug("핫 캐시 조회: 요청={}명, 히트={}명", userIds.size(), result.size());

        } catch (Exception e) {
            log.warn("핫 캐시 조회 실패: {}", e.getMessage());
        }

        return result;
    }

    private Map<String, String> getFromWarmCache(List<String> userIds) {
        var result = new HashMap<String, String>();

        try {
            var keys = userIds.stream()
                    .map(userId -> DrivingVicinityPolicy.TRAIT_WARM_PREFIX + userId)
                    .toList();

            var values = redisTemplate.opsForValue().multiGet(keys);

            for (int i = 0; i < userIds.size(); i++) {
                var value = values.get(i);
                if (value != null) {
                    var character = parseCharacter(value);
                    if (character != null) {
                        result.put(userIds.get(i), character);
                    }
                }
            }

            log.debug("워밍 캐시 조회: 요청={}명, 히트={}명", userIds.size(), result.size());

        } catch (Exception e) {
            log.warn("워밍 캐시 조회 실패: {}", e.getMessage());
        }

        return result;
    }

    public void saveToHotCache(Map<String, String> traits) {
        if (traits == null || traits.isEmpty()) {
            return;
        }

        try {
            traits.forEach((userId, character) -> {
                try {
                    var key = DrivingVicinityPolicy.TRAIT_HOT_PREFIX + userId;
                    var value = createCacheValue(character);
                    redisTemplate.opsForValue().set(key, value, DrivingVicinityPolicy.HOT_CACHE_TTL_SEC, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.warn("핫 캐시 개별 저장 실패: userId={}, 오류={}", userId, e.getMessage());
                }
            });

            log.debug("핫 캐시 저장 완료: {}명", traits.size());

        } catch (Exception e) {
            log.warn("핫 캐시 저장 실패: {}", e.getMessage());
        }
    }

    public void saveToWarmCache(Map<String, String> traits) {
        if (traits == null || traits.isEmpty()) {
            return;
        }

        try {
            traits.forEach((userId, character) -> {
                try {
                    var key = DrivingVicinityPolicy.TRAIT_WARM_PREFIX + userId;
                    var value = createCacheValue(character);
                    redisTemplate.opsForValue().set(key, value, DrivingVicinityPolicy.WARM_CACHE_TTL_SEC, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.warn("워밍 캐시 개별 저장 실패: userId={}, 오류={}", userId, e.getMessage());
                }
            });

            log.debug("워밍 캐시 저장 완료: {}명", traits.size());

        } catch (Exception e) {
            log.warn("워밍 캐시 저장 실패: {}", e.getMessage());
        }
    }

    private String createCacheValue(String character) {
        return "{\"v\":1,\"character\":\"%s\"}".formatted(character);
    }

    private String parseCharacter(String cacheValue) {
        try {
            if (cacheValue.contains("\"character\":")) {
                var start = cacheValue.indexOf("\"character\":\"") + 13;
                var end = cacheValue.indexOf("\"", start);
                if (start > 12 && end > start) {
                    return cacheValue.substring(start, end);
                }
            }
        } catch (Exception e) {
            log.warn("캐시 값 파싱 실패: value={}", cacheValue);
        }
        return null;
    }
}
