package com.smooth.drivecast_service.global.config;

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

    public SessionEventListener(@Qualifier("stringRedisTemplate") StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    // private final JwtTokenProvider jwtTokenProvider;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        // 클라이언트에서 보낸 userId 추출(with Authorization)
        // String token = accessor.getFirstNativeHeader("Authorization");

        String userId = accessor.getFirstNativeHeader("userId");
        String sessionId = accessor.getSessionId();

        if (userId == null || userId.isBlank()) {
            log.warn("WebSocket 연결 시 userId 누락 (sessionId={})", sessionId);
            return;
        }
        if (sessionId == null) {
            log.warn("WebSocket 연결 시 sessionId 누락 (userId={})", userId);
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

        log.info("[CONNECT] userId={}, sessionId={}", userId, sessionId);

//        if (token != null && token.startsWith("Bearer ")) {
//            token = token.substring(7);
//            try {
//                String userId = jwtTokenProvider.getUserId(token);
//                String sessionId = accessor.getSessionId();
//
//                if (sessionId == null) {
//                    log.warn("SessionId가 null입니다. userId={}", userId);
//                    return;
//                }
//
//                // 기존 세션이 있다면 정리
//                String existingSessionKey = "session:" + userId;
//                String existingSessionId = redisTemplate.opsForValue().get(existingSessionKey);
//                if (existingSessionId != null) {
//                    redisTemplate.delete("user:" + existingSessionId);
//                }
//
//                // 새 세션 매핑 저장
//                redisTemplate.opsForValue().set(existingSessionKey, sessionId, Duration.ofHours(3));
//                redisTemplate.opsForValue().set("user:" + sessionId, userId, Duration.ofHours(3));
//
//                log.info("WebSocket CONNECT: userId={}, sessionId={}", userId, sessionId);
//            } catch (Exception e) {
//                log.warn("JWT 파싱 실패: {}", e.getMessage());
//            }
//        } else {
//            log.warn("Authorization 헤더 없음");
//        }
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
