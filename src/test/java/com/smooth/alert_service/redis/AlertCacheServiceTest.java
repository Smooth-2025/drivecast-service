package com.smooth.alert_service.redis;

import com.smooth.alert_service.repository.AlertCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AlertCacheServiceTest {

    @SuppressWarnings("unchecked")
    RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
    @SuppressWarnings("unchecked")
    ValueOperations<String, String> valueOps = mock(ValueOperations.class);

    AlertCacheService cacheService = new AlertCacheService(redisTemplate);

    @Test
    void 알림이_이미_전송되었는지_확인한다() {
        when(redisTemplate.hasKey("alert:acc-123:user-456")).thenReturn(true);

        boolean result = cacheService.isAlreadySent("acc-123", "user-456");

        assertThat(result).isTrue();
    }

    @Test
    void 알림_전송_이력을_저장한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        cacheService.markAsSent("acc-123", "user-456");

        verify(valueOps, times(1))
                .set(eq("alert:acc-123:user-456"), eq("sent"), any());
    }
}