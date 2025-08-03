package com.smooth.alert_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestAlertController {

    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/test-alert")
    public ResponseEntity<String> sendTestAlert(@RequestParam String userId) {
        String message = "🚨 테스트 알림: 서버에서 " + userId + "에게 전송됨";
        messagingTemplate.convertAndSendToUser(userId, "/alert", message);
        return ResponseEntity.ok("✅ 전송됨: " + userId);
    }
}
