package com.smooth.drivecast_service.global.common.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smooth.drivecast_service.global.common.pod.PodInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalConnectionManager {

    @Qualifier("messagingStringRedisTemplate")
    private final StringRedisTemplate messagingRedisTemplate;
    private final ObjectMapper objectMapper;
    private final PodInfo podInfo;
    
    private static final String GLOBAL_CONNECTION_KEY_PREFIX = "ws:global:connection:";
    private static final String KICK_CHANNEL = "ws:system:kick";
    private static final Duration CONNECTION_TTL = Duration.ofMinutes(5); // 5분 TTL

    public String registerGlobalConnection(String userId) {
        String currentPodId = podInfo.getPodId();
        String connectionKey = GLOBAL_CONNECTION_KEY_PREFIX + userId;
        
        try {
            String existingPodId = messagingRedisTemplate.opsForValue().get(connectionKey);
            
            if (existingPodId != null && !existingPodId.equals(currentPodId)) {
                sendKickSignal(userId, existingPodId, "새로운 연결로 인한 기존 연결 해제");
                log.info("전역 중복 연결 감지: userId={}, 기존Pod={}, 현재Pod={}", 
                    userId, existingPodId, currentPodId);
            }

            messagingRedisTemplate.opsForValue().set(connectionKey, currentPodId, CONNECTION_TTL);
            log.debug("전역 연결 등록: userId={}, podId={}", userId, currentPodId);
            
            return existingPodId;
            
        } catch (Exception e) {
            log.error("전역 연결 등록 실패: userId={}", userId, e);
            return null;
        }
    }

    public void unregisterGlobalConnection(String userId) {
        String currentPodId = podInfo.getPodId();
        String connectionKey = GLOBAL_CONNECTION_KEY_PREFIX + userId;
        
        try {
            String registeredPodId = messagingRedisTemplate.opsForValue().get(connectionKey);

            if (currentPodId.equals(registeredPodId)) {
                messagingRedisTemplate.delete(connectionKey);
                log.debug("전역 연결 해제: userId={}, podId={}", userId, currentPodId);
            }
            
        } catch (Exception e) {
            log.error("전역 연결 해제 실패: userId={}", userId, e);
        }
    }

    public void refreshConnection(String userId) {
        String currentPodId = podInfo.getPodId();
        String connectionKey = GLOBAL_CONNECTION_KEY_PREFIX + userId;
        
        try {
            String registeredPodId = messagingRedisTemplate.opsForValue().get(connectionKey);

            if (currentPodId.equals(registeredPodId)) {
                messagingRedisTemplate.expire(connectionKey, CONNECTION_TTL);
                log.debug("연결 TTL 갱신: userId={}, podId={}", userId, currentPodId);
            }
            
        } catch (Exception e) {
            log.error("연결 TTL 갱신 실패: userId={}", userId, e);
        }
    }

    private void sendKickSignal(String userId, String targetPodId, String reason) {
        try {
            KickMessage kickMessage = new KickMessage(userId, targetPodId, reason, podInfo.getPodId());
            String messageJson = objectMapper.writeValueAsString(kickMessage);
            
            messagingRedisTemplate.convertAndSend(KICK_CHANNEL, messageJson);
            log.info("킥 신호 전송: userId={}, targetPod={}, reason={}", userId, targetPodId, reason);
            
        } catch (Exception e) {
            log.error("킥 신호 전송 실패: userId={}, targetPod={}", userId, targetPodId, e);
        }
    }

    public static class KickMessage {
        public String userId;
        public String targetPodId;
        public String reason;
        public String sourcePodId;
        
        public KickMessage(String userId, String targetPodId, String reason, String sourcePodId) {
            this.userId = userId;
            this.targetPodId = targetPodId;
            this.reason = reason;
            this.sourcePodId = sourcePodId;
        }
    }
}