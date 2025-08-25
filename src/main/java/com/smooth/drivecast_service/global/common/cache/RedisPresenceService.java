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
            stringRedisTemplate.opsForValue().set(key, String.valueOf(when.toEpochMilli()));
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
                .map(seen -> {
                    // 시계 스큐를 고려한 범위 체크: [refTime-skew, refTime+skew]
                    // 미래 시각 허용은 서버간 시계 차이를 고려한 설계
                    return !seen.isBefore(refTime.minus(skew)) && !seen.isAfter(refTime.plus(skew));
                })
                .orElse(false);
    }
}
