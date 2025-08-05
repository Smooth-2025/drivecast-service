package com.smooth.alert_service.core;

import com.smooth.alert_service.model.AlertEvent;
import com.smooth.alert_service.repository.AlertCacheService;
import com.smooth.alert_service.support.util.AlertIdResolver;
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
        Instant endTime = Instant.now().plus(Duration.ofMinutes(3));

        var scheduledFuture = scheduler.scheduleAtFixedRate(() -> {
            try {
                if (Instant.now().isAfter(endTime)) {
                    log.info("알림 반복 종료: alertId={}", alertId);
                    return;
                }
                
                List<String> nearbyUsers = vicinityUserFinder.findNearbyUsers(event);
                for (String userId : nearbyUsers) {
                    if (alertCacheService.isAlreadySent(alertId, userId)) continue;

                    log.info("알림 전송 예정(전송 로직은 T5.1.6에서 구현): alertId={}, userId={}", alertId, userId);
                    alertCacheService.markAsSent(alertId, userId);
                }
            } catch (Exception e) {
                log.error("알림 반복 처리 중 오류 발생: alertId={}", alertId, e);
            }
        }, 0, 10, TimeUnit.SECONDS);

        // 3분 후 스케줄러 자동 종료
        scheduler.schedule(() -> {
            scheduledFuture.cancel(false);
            log.info("알림 반복 스케줄러 종료: alertId={}", alertId);
        }, 3, TimeUnit.MINUTES);
    }
}
