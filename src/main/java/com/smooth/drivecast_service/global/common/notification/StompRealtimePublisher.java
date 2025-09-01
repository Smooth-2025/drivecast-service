package com.smooth.drivecast_service.global.common.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompRealtimePublisher implements RealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void toUser(String userId, String destination, Object payload) {
        if (userId == null || userId.isBlank()) {
            log.warn("userId가 없어 메시지 전송 스킵: destination={}", destination);
            return;
        }

        try {
            messagingTemplate.convertAndSendToUser(userId, destination, payload);
            log.debug("실시간 메시지 전송 성공: userId={}, destination={}", userId, destination);
        } catch (Exception e) {
            log.error("실시간 메시지 전송 실패: userId={}, destination={}", userId, destination, e);
        }
    }
}
