package com.smooth.drivecast_service.global.config;

import com.smooth.drivecast_service.global.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Duration;

@Slf4j
@Component
public class SessionEventListener {

    private final StringRedisTemplate redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    public SessionEventListener(@Qualifier("stringRedisTemplate") StringRedisTemplate redisTemplate,
                               JwtTokenProvider jwtTokenProvider) {
        this.redisTemplate = redisTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        // JWT 토큰에서 userId 추출 (1인 1룸 형태)
        String token = accessor.getFirstNativeHeader("Authorization");
        String sessionId = accessor.getSessionId();

        if (sessionId == null) {
            log.warn("WebSocket 연결 시 sessionId 누락");
            return;
        }

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                String userId = jwtTokenProvider.getUserId(token);

                // 1인 1룸: 기존 세션이 있다면 정리 (중복 연결 방지)
                String existingSessionKey = "session:" + userId;
                String existingSessionId = redisTemplate.opsForValue().get(existingSessionKey);
                if (existingSessionId != null && !existingSessionId.equals(sessionId)) {
                    // 기존 세션 정리
                    redisTemplate.delete("user:" + existingSessionId);
                    log.info("기존 세션 정리: userId={}, oldSessionId={}", userId, existingSessionId);
                }

                // 새 세션 매핑 저장 (1인 1룸)
                redisTemplate.opsForValue().set(existingSessionKey, sessionId, Duration.ofHours(3));
                redisTemplate.opsForValue().set("user:" + sessionId, userId, Duration.ofHours(3));

                log.info("[CONNECT] userId={}, sessionId={} (1인 1룸)", userId, sessionId);
            } catch (Exception e) {
                log.warn("JWT 파싱 실패: sessionId={}, error={}", sessionId, e.getMessage());
                // JWT 파싱 실패 시 연결 거부하지 않고 경고만 로그
            }
        } else {
            log.warn("Authorization 헤더가 없거나 Bearer 토큰이 아닙니다: sessionId={}", sessionId);
            // 토큰 없어도 연결은 허용하되 경고 로그
        }
    }

    @EventListener
    public void handleWebsocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();

        if (sessionId == null) {
            log.warn("DISCONNECT 이벤트에서 SessionId가 null입니다.");
            return;
        }

        String userId = redisTemplate.opsForValue().get("user:" + sessionId);
        if (userId != null) {
            redisTemplate.delete("session:" + userId);
            redisTemplate.delete("user:" + sessionId);
            log.info("WebSocket DISCONNECT: userId={}, sessionId={}", userId, sessionId);
        } else {
            log.info("WebSocket DISCONNECT: sessionId={} (사용자 정보 없음)", sessionId);
        }
    }
}
