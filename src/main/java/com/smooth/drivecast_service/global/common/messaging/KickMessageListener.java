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

            GlobalConnectionManager.KickMessage kickMessage = 
                objectMapper.readValue(messageBody, GlobalConnectionManager.KickMessage.class);

            String currentPodId = podInfo.getPodId();
            
            if (!currentPodId.equals(kickMessage.targetPodId)) {
                log.debug("다른 Pod 대상 킥 신호 무시: targetPod={}, currentPod={}", 
                    kickMessage.targetPodId, currentPodId);
                return;
            }

            if (currentPodId.equals(kickMessage.sourcePodId)) {
                log.debug("자신이 보낸 킥 신호 무시: userId={}, podId={}", 
                    kickMessage.userId, currentPodId);
                return;
            }

            if (localConnectionManager.hasConnection(kickMessage.userId)) {
                if (messagingTemplate == null) {
                    messagingTemplate = applicationContext.getBean(SimpMessagingTemplate.class);
                }

                sendKickNotification(kickMessage.userId, kickMessage.reason);
                
                log.info("사용자 킥 처리 완료: userId={}, reason={}", 
                    kickMessage.userId, kickMessage.reason);
            } else {
                log.debug("킥 대상 사용자 로컬 연결 없음: userId={}", kickMessage.userId);
            }
            
        } catch (Exception e) {
            log.error("킥 신호 처리 실패", e);
        }
    }

    private void sendKickNotification(String userId, String reason) {
        try {
            var kickNotification = new KickNotification("CONNECTION_REPLACED", reason);
            messagingTemplate.convertAndSendToUser(userId, "/queue/system", kickNotification);
            
            log.debug("킥 알림 전송 완료: userId={}, reason={}", userId, reason);
            
        } catch (Exception e) {
            log.error("킥 알림 전송 실패: userId={}", userId, e);
        }
    }

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