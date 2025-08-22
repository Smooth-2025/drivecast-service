package com.smooth.drivecast_service.core;

import com.smooth.drivecast_service.model.AlertType;
import com.smooth.drivecast_service.model.AlertEvent;
import com.smooth.drivecast_service.support.validator.AlertEventValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertEventHandler {

    private final AlertRepeatNotifier alertRepeatNotifier;
    private final AlertSender alertSender;

    public void handle(AlertEvent event) {
        log.info("알림 이벤트 수신: type={}, userId={}, time={}", event.type(), event.userId(), event.timestamp());

        try {
            AlertEventValidator.validate(event);

            var alertType = AlertType.from(event.type());
            if (alertType.isEmpty()) {
                log.warn("알 수 없는 알림 타입: {}", event.type());
                return;
            }

            // 타입별 명확한 분기 처리
            switch (alertType.get()) {
                case ACCIDENT, OBSTACLE -> {
                    // 반복 알림: 초기 전송 + 3분간 새 진입자 감지
                    log.info("반복 알림 시작: type={}", event.type());
                    alertRepeatNotifier.start(event);
                }
                case START, END -> {
                    // 1회성 알림: 본인에게만
                    log.info("1회성 알림 (본인): type={}", event.type());
                    sendToSelf(event);
                }
            }

        } catch (IllegalArgumentException e) {
            log.error("잘못된 이벤트 데이터: {}", e.getMessage());
        } catch (Exception e) {
            log.error("이벤트 처리 중 오류 발생", e);
        }
    }

    // START/END용: 본인에게만 전송
    private void sendToSelf(AlertEvent event) {
        if (event.userId() == null) {
            log.warn("userId 없음: {}", event);
            return;
        }

        AlertMessageMapper.map(event, event.userId()).ifPresent(msg -> {
            alertSender.sendToUser(event.userId(), msg);
            log.info("본인 알림 전송 완료: type={}, userId={}", event.type(), event.userId());
        });
    }
}
