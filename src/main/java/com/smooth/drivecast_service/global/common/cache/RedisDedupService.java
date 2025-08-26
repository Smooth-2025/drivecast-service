package com.smooth.drivecast_service.global.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisDedupService implements DedupService {

    @Qualifier("stringRedisTemplate")
    private final RedisTemplate<String, String> stringRedisTemplate;

    @Override
    public boolean markIfFirst(String key, Duration ttl) {
        if (key == null || key.isBlank()) {
            log.warn("중복 방지 키가 null 또는 빈 문자열입니다.");
            return false;
        }

        try {
            Boolean isFirst = stringRedisTemplate.opsForValue().setIfAbsent(key, "processed", ttl);
            boolean result = Boolean.TRUE.equals(isFirst);

            if (result) {
                log.debug("첫 번째 요청 처리: key={}, ttl={}", key, ttl);
            } else {
                log.debug("중복 요청 스킵: key={}", key);
            }

            return result;
        } catch (Exception e) {
            log.error("중복 방지 처리 실패: key={}, ttl={}", key, ttl, e);
            return false;
        }
    }

    @Override
    public boolean isAlreadyProcessed(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }

        try {
            return stringRedisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("중복 확인 실패: key={}", key, e);
            return false;
        }
    }
}
