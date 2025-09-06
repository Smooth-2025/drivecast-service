package com.smooth.drivecast_service.global.common.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smooth.drivecast_service.global.common.pod.PodInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 킥 신호를 수신하여 해당 사용자 연결을 강제 해제하는 리스너
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.redis.enabled", havingValue = "true", matchIfMissing = true)
public class KickMessageListener implements MessageListener {

    private final LocalConnectionManager localConnectionManager;
    private final ObjectMapper objectMapper;
    private final ApplicationContext applicationContext;
    private final PodInfo podInfo;
    
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String messageBody = new String(message.getBody());
            
            log.debug("킥 신호 수신: channel={}, message={}", channel, messageBody);
            
            // 메시지 파싱
            GlobalConnectionManager.KickMessage kickMessage = 
                objectMapper.readValue(messageBody, GlobalConnectionManager.KickMessage.class);
            
            // 현재 Pod가 대상인지 확인
            String currentPodId = podInfo.getPodId();
            
            if (!currentPodId.equals(kickMessage.targetPodId)) {
                log.debug("다른 Pod 대상 킥 신호 무시: targetPod={}, currentPod={}", 
                    kickMessage.targetPodId, currentPodId);
                return;
            }
            
            // 자신이 보낸 킥 신호는 무시
            if (currentPodId.equals(kickMessage.sourcePodId)) {
                log.debug("자신이 보낸 킥 신호 무시: userId={}, podId={}", 
                    kickMessage.userId, currentPodId);
                return;
            }
            
            // 로컬 연결이 있는 경우 강제 해제
            if (localConnectionManager.hasConnection(kickMessage.userId)) {
                // Lazy initialization to avoid circular dependency
                if (messagingTemplate == null) {
                    messagingTemplate = applicationContext.getBean(SimpMessagingTemplate.class);
                }
                
                // 사용자에게 킥 알림 전송
                sendKickNotification(kickMessage.userId, kickMessage.reason);
                
                // 로컬 연결 해제 (세션은 자동으로 정리됨)
                // LocalConnectionManager에서 직접 해제하지 않고, 클라이언트가 재연결하도록 유도
                
                log.info("사용자 킥 처리 완료: userId={}, reason={}", 
                    kickMessage.userId, kickMessage.reason);
            } else {
                log.debug("킥 대상 사용자 로컬 연결 없음: userId={}", kickMessage.userId);
            }
            
        } catch (Exception e) {
            log.error("킥 신호 처리 실패", e);
        }
    }
    
    /**
     * 사용자에게 킥 알림 전송
     */
    private void sendKickNotification(String userId, String reason) {
        try {
            var kickNotification = new KickNotification("CONNECTION_REPLACED", reason);
            messagingTemplate.convertAndSendToUser(userId, "/queue/system", kickNotification);
            
            log.debug("킥 알림 전송 완료: userId={}, reason={}", userId, reason);
            
        } catch (Exception e) {
            log.error("킥 알림 전송 실패: userId={}", userId, e);
        }
    }
    
    /**
     * 킥 알림 DTO
     */
    public static class KickNotification {
        public String type;
        public String message;
        public long timestamp;
        
        public KickNotification(String type, String message) {
            this.type = type;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
    }
}