package com.smooth.alert_service.core;

import com.smooth.alert_service.model.AlertType;
import com.smooth.alert_service.model.AlertEvent;
import com.smooth.alert_service.support.validator.AlertEventValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertEventHandler {

    private final AlertRepeatNotifier alertRepeatNotifier;

    public void handle(AlertEvent event) {
        log.info("알림 이벤트 수신: type={}, userId={}, time={}", event.type(), event.userId(), event.timestamp());

        try {
            AlertEventValidator.validate(event);
            
            var alertType = AlertType.from(event.type());
            if (alertType.isEmpty()) {
                log.warn("알 수 없는 알림 타입: {}", event.type());
                return;
            }

            if (alertType.get() == AlertType.ACCIDENT || alertType.get() == AlertType.OBSTACLE) {
                alertRepeatNotifier.start(event);
            } else {
                log.info("반복 전송 대상 아님: type={}", event.type());
            }
        } catch (IllegalArgumentException e) {
            log.error("잘못된 이벤트 데이터: {}", e.getMessage());
        } catch (Exception e) {
            log.error("이벤트 처리 중 오류 발생", e);
        }
    }
}
