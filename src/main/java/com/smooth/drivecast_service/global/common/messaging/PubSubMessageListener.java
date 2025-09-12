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
 * Redis Pub/Sub 메시지를 수신하여 로컬 WebSocket 연결로 전달하는 리스너
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.redis.enabled", havingValue = "true", matchIfMissing = true)
public class PubSubMessageListener implements MessageListener {

    private final LocalConnectionManager connectionManager;
    private final ObjectMapper objectMapper;
    private final ApplicationContext applicationContext;
    private final PodInfo podInfo;
    
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String messageBody = new String(message.getBody());
            
            log.debug("====Pub/Sub 메시지 수신: channel={}, message={}", channel, messageBody);

            // 메시지 파싱
            RealtimeMessage realtimeMessage = objectMapper.readValue(messageBody, RealtimeMessage.class);
            
            // 자신이 발행한 메시지는 무시 (중복 전송 방지)
            String currentPodId = podInfo.getPodId();
            if (currentPodId.equals(realtimeMessage.getSourcePodId())) {
                log.debug("====자신이 발행한 메시지 무시: userId={}, podId={}",
                    realtimeMessage.getUserId(), currentPodId);
                return;
            }
            
            // 로컬 연결이 있는 경우에만 전송
            if (connectionManager.hasConnection(realtimeMessage.getUserId())) {
                if (messagingTemplate == null) {
                    messagingTemplate = applicationContext.getBean(SimpMessagingTemplate.class);
                }
                
                messagingTemplate.convertAndSendToUser(
                    realtimeMessage.getUserId(),
                    realtimeMessage.getDestination(),
                    realtimeMessage.getPayload()
                );
                log.debug("====Pub/Sub 메시지 로컬 전송 완료: userId={}, destination={}",
                    realtimeMessage.getUserId(), realtimeMessage.getDestination());
            } else {
                log.debug("====로컬 연결 없음, 메시지 스킵: userId={}", realtimeMessage.getUserId());
            }
            
        } catch (Exception e) {
            log.error("====Pub/Sub 메시지 처리 실패", e);
        }
    }
}