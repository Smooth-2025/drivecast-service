package com.smooth.drivecast_service.global.common.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smooth.drivecast_service.global.common.messaging.LocalConnectionManager;
import com.smooth.drivecast_service.global.common.messaging.RealtimeMessage;
import com.smooth.drivecast_service.global.common.pod.PodInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import static com.smooth.drivecast_service.global.constants.GlobalConstants.Redis.WEBSOCKET_CHANNEL;

@Slf4j
@Component
public class StompRealtimePublisher implements RealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final LocalConnectionManager connectionManager;
    private final StringRedisTemplate messagingRedisTemplate;
    private final ObjectMapper objectMapper;
    private final PodInfo podInfo;
    
    public StompRealtimePublisher(SimpMessagingTemplate messagingTemplate,
                                LocalConnectionManager connectionManager,
                                @Qualifier("messagingStringRedisTemplate") StringRedisTemplate messagingRedisTemplate,
                                ObjectMapper objectMapper,
                                PodInfo podInfo) {
        this.messagingTemplate = messagingTemplate;
        this.connectionManager = connectionManager;
        this.messagingRedisTemplate = messagingRedisTemplate;
        this.objectMapper = objectMapper;
        this.podInfo = podInfo;
    }

    @Override
    public void toUser(String userId, String destination, Object payload) {
        if (userId == null || userId.isBlank()) {
            log.warn("userId가 없어 메시지 전송 스킵: destination={}", destination);
            return;
        }

        try {
            // 1. 로컬 연결 확인 및 직접 전송
            if (connectionManager.hasConnection(userId)) {
                messagingTemplate.convertAndSendToUser(userId, destination, payload);
                log.debug("✅ 로컬 실시간 메시지 전송 성공: userId={}, destination={}", userId, destination);
            }
            
            // 2. 항상 다른 Pod으로도 Pub/Sub 메시지 발행 (사용자가 어느 Pod에 있을지 모르므로)
            RealtimeMessage message = new RealtimeMessage(userId, destination, payload, podInfo.getPodId());
            String messageJson = objectMapper.writeValueAsString(message);
            messagingRedisTemplate.convertAndSend(WEBSOCKET_CHANNEL, messageJson);
            log.debug("✅ Pub/Sub 메시지 발행 완료: userId={}, destination={}", userId, destination);
            
        } catch (Exception e) {
            log.error("실시간 메시지 전송 실패: userId={}, destination={}", userId, destination, e);
        }
    }
}
