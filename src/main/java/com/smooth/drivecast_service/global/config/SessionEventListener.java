package com.smooth.drivecast_service.global.config;

import com.smooth.drivecast_service.global.common.messaging.LocalConnectionManager;
import com.smooth.drivecast_service.global.common.messaging.GlobalConnectionManager;
import com.smooth.drivecast_service.global.security.StompPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionEventListener {

    private final LocalConnectionManager localConnectionManager;
    private final GlobalConnectionManager globalConnectionManager;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        
        if (accessor.getUser() instanceof StompPrincipal principal) {
            String userId = principal.getName();
            String sessionId = accessor.getSessionId();
            
            // 1. 전역 연결 등록 (파드 간 1인 1룸 보장)
            String existingPodId = globalConnectionManager.registerGlobalConnection(userId);
            if (existingPodId != null) {
                log.info("파드 간 중복 연결 처리: userId={}, 기존Pod={}", userId, existingPodId);
            }
            
            // 2. 로컬 연결 등록
            localConnectionManager.addConnection(userId, sessionId);
            
            log.debug("WebSocket 연결 등록 완료: userId={}, sessionId={}", userId, sessionId);
        }
    }

    @EventListener
    public void handleWebsocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        
        if (sessionId != null) {
            // 1. 로컬 연결에서 userId 조회
            String userId = localConnectionManager.getUserBySession(sessionId);
            
            // 2. 로컬 연결 해제
            localConnectionManager.removeConnection(sessionId);
            
            // 3. 전역 연결 해제
            if (userId != null) {
                globalConnectionManager.unregisterGlobalConnection(userId);
            }
            
            log.debug("WebSocket 연결 해제 완료: sessionId={}, userId={}", sessionId, userId);
        }
    }
}
