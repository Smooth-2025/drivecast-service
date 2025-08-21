package com.smooth.alert_service.support.util;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
public class LastSeenService {
    private final StringRedisTemplate redis;

    public LastSeenService(@Qualifier("stringRedisTemplate") StringRedisTemplate redis) {
        this.redis = redis;
    }

    private String key(String userId) {
        return "lastseen:" + userId;
    }

    /** 사용자 마지막 갱신 시각(UTC)을 epochMilli로 저장 */
    public void markSeen(String userId, Instant when) {
        if (userId == null || when == null) return;
        redis.opsForValue().set(key(userId), String.valueOf(when.toEpochMilli()));
    }

    /** 마지막 갱신 시각(UTC) 조회 */
    public Optional<Instant> getLastSeen(String userId) {
        String v = redis.opsForValue().get(key(userId));
        if (v == null) return Optional.empty();
        try {
            return Optional.of(Instant.ofEpochMilli(Long.parseLong(v)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** refTime ± skew 안에 있으면 true */
    public boolean isFresh(String userId, Instant refTime, Duration skew) {
        return getLastSeen(userId)
                .map(seen -> !seen.isBefore(refTime.minus(skew)) && !seen.isAfter(refTime.plus(skew)))
                .orElse(false);
    }
}
