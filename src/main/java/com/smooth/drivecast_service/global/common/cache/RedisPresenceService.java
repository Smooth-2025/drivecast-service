package com.smooth.drivecast_service.global.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisPresenceService implements PresenceService {

    @Qualifier("stringRedisTemplate")
    private final StringRedisTemplate stringRedisTemplate;

    private String buildKey(String userId) {
        return "lastseen:" + userId;
    }

    @Override
    public void markSeen(String userId, Instant when) {
        if(userId == null || when == null) return;

        try {
            String key = buildKey(userId);
            stringRedisTemplate.opsForValue().set(key, String.valueOf(when.toEpochMilli()), Duration.ofHours(1));
            log.debug("사용자 접속 기록: userId={}, when={}", userId, when);
        } catch (Exception e) {
            log.error("사용자 접속 기록 실패: userId={}, when={}", userId, when, e);
        }
    }

    @Override
    public Optional<Instant> getLastSeen(String userId) {
        try {
            String key = buildKey(userId);
            String value = stringRedisTemplate.opsForValue().get(key);
            if (value == null) return Optional.empty();

            return Optional.of(Instant.ofEpochMilli(Long.parseLong(value)));
        } catch (Exception e) {
            log.error("사용자 접속 조회 실패: userId={}", userId, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean isFresh(String userId, Instant refTime, Duration skew) {
        return getLastSeen(userId)
                .map(seen -> isWithinSkewRange(seen, refTime, skew))
                .orElse(false);
    }

    private boolean isWithinSkewRange(Instant seen, Instant refTime, Duration skew) {
        Instant minTime = refTime.minus(skew);
        Instant maxTime = refTime.plus(skew);
        return !seen.isBefore(minTime) && !seen.isAfter(maxTime);
    }
}
