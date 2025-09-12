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

import static com.smooth.drivecast_service.global.constants.GlobalConstants.Redis.*;

/**
 * 파드 간 전역 WebSocket 연결 관리
 * 클러스터 전체에서 1인 1룸 보장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalConnectionManager {

    @Qualifier("messagingStringRedisTemplate")
    private final StringRedisTemplate messagingRedisTemplate;
    private final ObjectMapper objectMapper;
    private final PodInfo podInfo;
    
    /**
     * 전역 연결 등록 및 중복 체크
     * @return 기존 연결이 있었다면 해당 Pod ID, 없었다면 null
     */
    public String registerGlobalConnection(String userId) {
        String currentPodId = podInfo.getPodId();
        String connectionKey = CONNECTION_KEY_PREFIX + userId;
        
        try {
            // 기존 연결 확인
            String existingPodId = messagingRedisTemplate.opsForValue().get(connectionKey);
            
            if (existingPodId != null && !existingPodId.equals(currentPodId)) {
                // 다른 Pod에 연결되어 있음 - 킥 신호 전송
                sendKickSignal(userId, existingPodId, "새로운 연결로 인한 기존 연결 해제");
                log.info("전역 중복 연결 감지: userId={}, 기존Pod={}, 현재Pod={}", 
                    userId, existingPodId, currentPodId);
            }
            
            // 현재 Pod로 등록 (TTL 설정)
            messagingRedisTemplate.opsForValue().set(connectionKey, currentPodId, CONNECTION_TTL);
            log.debug("전역 연결 등록: userId={}, podId={}", userId, currentPodId);
            
            return existingPodId;
            
        } catch (Exception e) {
            log.error("전역 연결 등록 실패: userId={}", userId, e);
            return null;
        }
    }
    
    /**
     * 전역 연결 해제
     */
    public void unregisterGlobalConnection(String userId) {
        String currentPodId = podInfo.getPodId();
        String connectionKey = CONNECTION_KEY_PREFIX + userId;
        
        try {
            String registeredPodId = messagingRedisTemplate.opsForValue().get(connectionKey);
            
            // 현재 Pod에 등록된 경우에만 해제
            if (currentPodId.equals(registeredPodId)) {
                messagingRedisTemplate.delete(connectionKey);
                log.debug("전역 연결 해제: userId={}, podId={}", userId, currentPodId);
            }
            
        } catch (Exception e) {
            log.error("전역 연결 해제 실패: userId={}", userId, e);
        }
    }
    
    /**
     * 연결 TTL 갱신 (하트비트)
     */
    public void refreshConnection(String userId) {
        String currentPodId = podInfo.getPodId();
        String connectionKey = CONNECTION_KEY_PREFIX + userId;
        
        try {
            String registeredPodId = messagingRedisTemplate.opsForValue().get(connectionKey);
            
            // 현재 Pod에 등록된 경우에만 TTL 갱신
            if (currentPodId.equals(registeredPodId)) {
                messagingRedisTemplate.expire(connectionKey, CONNECTION_TTL);
                log.debug("연결 TTL 갱신: userId={}, podId={}", userId, currentPodId);
            }
            
        } catch (Exception e) {
            log.error("연결 TTL 갱신 실패: userId={}", userId, e);
        }
    }
    
    /**
     * 킥 신호 전송
     */
    private void sendKickSignal(String userId, String targetPodId, String reason) {
        try {
            KickMessage kickMessage = new KickMessage(userId, targetPodId, reason, podInfo.getPodId());
            String messageJson = objectMapper.writeValueAsString(kickMessage);
            
            messagingRedisTemplate.convertAndSend(KICK_CHANNEL, messageJson);
            log.info("✅ 킥 신호 전송: userId={}, targetPod={}, reason={}", userId, targetPodId, reason);
            
        } catch (Exception e) {
            log.error("❌ 킥 신호 전송 실패: userId={}, targetPod={}", userId, targetPodId, e);
        }
    }
    

    
    /**
     * 킥 메시지 DTO
     */
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