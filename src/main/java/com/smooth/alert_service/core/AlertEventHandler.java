package com.smooth.alert_service.core;

import com.smooth.alert_service.model.AlertType;
import com.smooth.alert_service.model.AlertEvent;
import com.smooth.alert_service.model.EventType;
import com.smooth.alert_service.support.validator.AlertEventValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertEventHandler {

    private final AlertRepeatNotifier alertRepeatNotifier;
    private final VicinityUserFinder vicinityUserFinder;
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
                case POTHOLE -> {
                    // 1회성 알림: 반경 내 모든 사용자
                    log.info("1회성 알림 (반경): type={}", event.type());
                    sendToRadius(event);
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

    // POTHOLE용: 반경 내 사용자들에게 전송
    private void sendToRadius(AlertEvent event) {
        var eventType = EventType.from(event.type())
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 알림 타입: " + event.type()));

        if (event.latitude() == null || event.longitude() == null) {
            log.warn("좌표 정보 없음: {}", event);
            return;
        }

        // TODO: POTHOLE 중복 방지 로직 추가 필요 (플링크 구현 완료 후)
        // - AlertCacheService.markIfFirst() 호출 추가
        // - alertId 생성 방식: 좌표 기반으로 변경 (같은 위치 포트홀 = 같은 ID)
        // - 현재는 AlertIdResolver에서 매번 새 UUID 생성하여 중복 방지 불가능

        Instant refTime;
        try {
            refTime = Instant.parse(event.timestamp());
        } catch (Exception e) {
            log.warn("timestamp 파싱 실패, 현재 시각 사용: {}", event.timestamp());
            refTime = Instant.now();
        }
        List<String> targets = vicinityUserFinder.findUsersAround(
                event.latitude(),
                event.longitude(),
                eventType.getRadiusMeters(),
                refTime,
                Duration.ofSeconds(5),
                null // POTHOLE은 본인 제외 없음(무조건 전송)
        );

        for (String userId : targets) {
            AlertMessageMapper.map(event, userId).ifPresent(msg -> {
                alertSender.sendToUser(userId, msg);
                log.info("반경 알림 전송: type={}, userId={}", event.type(), userId);
            });
        }

        log.info("반경 알림 전송 완료: type={}, 대상수={}", event.type(), targets.size());
    }
}
