package com.smooth.drivecast_service.api.ws;

import com.smooth.drivecast_service.core.AlertSender;
import com.smooth.drivecast_service.model.AlertMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;

@Slf4j
@Controller
@RequiredArgsConstructor
public class StompMessageController {

    private final AlertSender alertSender;

    /** 헬스 체크: 클라가 /app/ping 으로 publish */
    @MessageMapping("/ping")
    public void ping(@Header(name = "simpSessionId", required = false) String sessionId,
                     Principal principal) {
        log.info("STOMP ping 수신 - sessionId={}, principal={}",
                sessionId, principal != null ? principal.getName() : "anonymous");
    }

    /**
     * 클라가 /app/alert 로 publish → 서버가 특정 유저에게 /user/alert 로 push
     * payload 예:
     * {
     *   "type":"test",
     *   "payload":{"userId":"driver1234","title":"테스트","content":"서버 라우팅"}
     * }
     */
    @MessageMapping("/alert")
    public void relayToUser(@Payload AlertMessageDto message, Principal principal) {
        log.info("STOMP alert 수신: {}", message);

        // 테스트 단계: payload.userId 우선. (jwt 포함시에는 principal만 사용)
        String userId = null;
        if (message != null && message.payload() != null && message.payload().get("userId") != null) {
            userId = String.valueOf(message.payload().get("userId"));
        }
        if (userId == null && principal != null) {
            userId = principal.getName();
        }
        if (userId == null || userId.isBlank()) {
            log.warn("userId가 없어 메시지 전송 스킵");
            return;
        }

        // timestamp 보강
        if (message.payload() != null && !message.payload().containsKey("timestamp")) {
            message.payload().put("timestamp", Instant.now().toString());
        }

        // 최종 전송: 클라는 /user/alert 구독
        alertSender.sendToUser(userId, message);
        log.info("STOMP → /user/alert 전송 완료: userId={}, type={}", userId, message.type());
    }
}
