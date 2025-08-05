package com.smooth.alert_service.support.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class LatestLocationKeyFinder {

    private final RedisTemplate<String, String> redisTemplate;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.ofHours(9));

    public String findLatestKey(Instant baseTime, int maxSeconds) {
        for (int i = 0; i <= maxSeconds; i++) {
            Instant time = baseTime.minusSeconds(i);
            String key = "location:" + FORMATTER.format(time);
            if (redisTemplate.hasKey(key)) {
                return key;
            }
        }
        return null;
    }
}
