package com.smooth.drivecast_service.core;

import com.smooth.drivecast_service.model.AlertMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertSender {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendToUser(String userId, AlertMessageDto message) {
        log.info("AlertSender.sendToUser 호출: userId={}", userId);
        try {
            messagingTemplate.convertAndSendToUser(userId, "/queue/alert", message);
            log.info("알림 전송 성공: userId={}", userId);
        } catch (Exception e) {
            log.error("알림 전송 실패: userId={}", userId, e);
        }
    }
}
