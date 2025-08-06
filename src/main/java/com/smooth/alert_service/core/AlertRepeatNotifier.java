package com.smooth.alert_service.core;

import com.smooth.alert_service.model.AlertEvent;
import com.smooth.alert_service.model.EventType;
import com.smooth.alert_service.repository.AlertCacheService;
import com.smooth.alert_service.support.util.AlertIdResolver;
import com.smooth.alert_service.support.util.LatestLocationKeyFinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertRepeatNotifier {

    private final VicinityUserFinder vicinityUserFinder;
    private final AlertCacheService alertCacheService;
    private final LatestLocationKeyFinder latestLocationKeyFinder;
    private final AlertSender alertSender;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
            2,
            new CustomizableThreadFactory("alert-repeat-")
    );

    public void start(AlertEvent event) {
        Optional<String> alertIdOpt = AlertIdResolver.resolve(event);
        if(alertIdOpt.isEmpty()) {
            log.info("반복 알림 제외 대상: type={}, userId={}", event.type(), event.userId());
            return;
        }

        String alertId = alertIdOpt.get();

        int radius = EventType.from(event.type())
                .map(EventType::getRadiusMeters)
                .orElse(0);

        // 1차 조회: 사고 발생 시점의 정확한 timestamp 키 사용
        performInitialNotification(event, alertId, radius);

        // 2차 이후: 최신 키로 반복 조회
        scheduleRepeatedNotification(event, alertId, radius);
    }

    private void performInitialNotification(AlertEvent event, String alertId, int radius) {
        try {
            String initialKey = "location:" + event.timestamp();
            log.info("초기 알림 조회 시작: alertId={}, key={}", alertId, initialKey);

            // OBSTACLE의 경우 본인 제외, ACCIDENT의 경우 본인 포함
            String excludeUserId = "obstacle".equals(event.type()) ? event.userId() : null;

            List<String> targetUsers = vicinityUserFinder.findUsersAround(
                    event.latitude(),
                    event.longitude(),
                    initialKey,
                    radius,
                    excludeUserId
            );

            for (String userId : targetUsers) {
                if (alertCacheService.isAlreadySent(alertId, userId)) continue;

                AlertMessageMapper.map(event, userId).ifPresent(msg -> {
                    alertSender.sendToUser(userId, msg);

                    boolean isSelf = event.userId() != null && event.userId().equals(userId);
                    String msgType = isSelf ? "내사고" : ("obstacle".equals(event.type()) ? "장애물" : "반경내사고");
                    log.info("🔥 초기 알림 전송 완료: type={}, userId={}, msgType={}, alertId={}",
                            event.type(), userId, msgType, alertId);
                });

                alertCacheService.markAsSent(alertId, userId);
            }
        } catch (Exception e) {
            log.error("초기 알림 처리 중 오류 발생: alertId={}", alertId, e);
        }
    }

    private void scheduleRepeatedNotification(AlertEvent event, String alertId, int radius) {
        Instant endTime = Instant.now().plus(Duration.ofMinutes(3));

        var scheduledFuture = scheduler.scheduleAtFixedRate(() -> {
            try {
                if (Instant.now().isAfter(endTime)) {
                    log.info("알림 반복 종료: alertId={}", alertId);
                    return;
                }

                String latestKey = latestLocationKeyFinder.findLatestKey(Instant.now(), 3);
                if (latestKey == null) {
                    log.warn("최근 위치 키 없음 (alertId={})", alertId);
                    return;
                }

                // 반복 알림에서는 항상 본인 제외 (새 진입자만 대상)
                List<String> nearbyUsers = vicinityUserFinder.findUsersAround(
                        event.latitude(),
                        event.longitude(),
                        latestKey,
                        radius,
                        event.userId()
                );

                for (String userId : nearbyUsers) {
                    if (alertCacheService.isAlreadySent(alertId, userId)) continue;

                    AlertMessageMapper.map(event, userId).ifPresent(msg -> {
                        alertSender.sendToUser(userId, msg);
                        log.info("🔄 반복 알림 전송 완료: type={}, userId={} (새 진입자), alertId={}",
                                event.type(), userId, alertId);
                    });

                    alertCacheService.markAsSent(alertId, userId);
                }

            } catch (Exception e) {
                log.error("반복 알림 처리 중 오류 발생: alertId={}", alertId, e);
            }
        }, 10, 10, TimeUnit.SECONDS);

        scheduler.schedule(() -> {
            scheduledFuture.cancel(false);
            log.info("알림 반복 스케줄러 종료: alertId={}", alertId);
        }, 3, TimeUnit.MINUTES);
    }
}
