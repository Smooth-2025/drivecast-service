package com.smooth.alert_service.config;

import com.smooth.alert_service.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionEventListener {

    private final StringRedisTemplate redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String token = accessor.getFirstNativeHeader("Authorization");

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                String userId = jwtTokenProvider.getUserId(token);
                String sessionId = accessor.getSessionId();

                if (sessionId == null) {
                    log.warn("❌ SessionId가 null입니다. userId={}", userId);
                    return;
                }

                // 기존 세션이 있다면 정리
                String existingSessionKey = "session:" + userId;
                String existingSessionId = redisTemplate.opsForValue().get(existingSessionKey);
                if (existingSessionId != null) {
                    redisTemplate.delete("user:" + existingSessionId);
                }

                // 새 세션 매핑 저장
                redisTemplate.opsForValue().set(existingSessionKey, sessionId, Duration.ofHours(3));
                redisTemplate.opsForValue().set("user:" + sessionId, userId, Duration.ofHours(3));
                
                log.info("✅ WebSocket CONNECT: userId={}, sessionId={}", userId, sessionId);
            } catch (Exception e) {
                log.warn("⚠️ JWT 파싱 실패: {}", e.getMessage());
            }
        } else {
            log.warn("❌ Authorization 헤더 없음");
        }
    }

    @EventListener
    public void handleWebsocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        
        if (sessionId == null) {
            log.warn("❌ DISCONNECT 이벤트에서 SessionId가 null입니다.");
            return;
        }

        String userId = redisTemplate.opsForValue().get("user:" + sessionId);
        if (userId != null) {
            redisTemplate.delete("session:" + userId);
            redisTemplate.delete("user:" + sessionId);
            log.info("🛑 WebSocket DISCONNECT: userId={}, sessionId={}", userId, sessionId);
        } else {
            log.info("🛑 WebSocket DISCONNECT: sessionId={} (사용자 정보 없음)", sessionId);
        }
    }
}
