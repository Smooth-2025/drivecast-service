package com.smooth.drivecast_service.global.common.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class LocalConnectionManager {

    private final ConcurrentHashMap<String, String> userSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionUsers = new ConcurrentHashMap<>();

    public void addConnection(String userId, String sessionId) {
        String existingSessionId = userSessions.put(userId, sessionId);
        sessionUsers.put(sessionId, userId);
        
        if (existingSessionId != null && !existingSessionId.equals(sessionId)) {
            sessionUsers.remove(existingSessionId);
            log.info("기존 연결 해제: userId={}, oldSession={}, newSession={}", 
                userId, existingSessionId, sessionId);
        }
        
        log.info("연결 등록: userId={}, sessionId={}", userId, sessionId);
    }

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

    public boolean hasConnection(String userId) {
        return userSessions.containsKey(userId);
    }

    public Set<String> getActiveConnections() {
        return userSessions.keySet();
    }

    public String getUserBySession(String sessionId) {
        return sessionUsers.get(sessionId);
    }
}