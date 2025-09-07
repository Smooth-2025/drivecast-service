package com.smooth.drivecast_service.global.common.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 로컬 파드 내 WebSocket 연결 관리
 * 1인 1룸 보장 (같은 파드 내)
 */
@Slf4j
@Component
public class LocalConnectionManager {

    private final ConcurrentHashMap<String, String> userSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionUsers = new ConcurrentHashMap<>();

    /**
     * 사용자 연결 추가 (SessionEventListener에서 호출)
     */
    public void addConnection(String userId, String sessionId) {
        String existingSessionId = userSessions.put(userId, sessionId);
        sessionUsers.put(sessionId, userId);
        
        if (existingSessionId != null && !existingSessionId.equals(sessionId)) {
            // 기존 연결 정리
            sessionUsers.remove(existingSessionId);
            log.info("기존 연결 해제: userId={}, oldSession={}, newSession={}", 
                userId, existingSessionId, sessionId);
        }
        
        log.info("연결 등록: userId={}, sessionId={}", userId, sessionId);
    }

    /**
     * 세션 연결 해제 (SessionEventListener에서 호출)
     */
    public void removeConnection(String sessionId) {
        String userId = sessionUsers.remove(sessionId);
        if (userId != null) {
            String currentSessionId = userSessions.get(userId);
            if (sessionId.equals(currentSessionId)) {
                userSessions.remove(userId);
            }
            log.info("연결 해제: userId={}, sessionId={}", userId, sessionId);
        }
    }

    /**
     * 사용자 연결 상태 확인
     */
    public boolean hasConnection(String userId) {
        return userSessions.containsKey(userId);
    }

    /**
     * 현재 연결된 사용자 목록
     */
    public Set<String> getActiveConnections() {
        return userSessions.keySet();
    }

    /**
     * 세션 ID로 사용자 ID 조회
     */
    public String getUserBySession(String sessionId) {
        return sessionUsers.get(sessionId);
    }
}