package com.smooth.drivecast_service.incident.service;

import com.smooth.drivecast_service.global.common.cache.DedupService;
import com.smooth.drivecast_service.global.common.location.VicinityService;
import com.smooth.drivecast_service.global.common.notification.RealtimePublisher;
import com.smooth.drivecast_service.incident.constants.IncidentDestinations;
import com.smooth.drivecast_service.incident.dto.IncidentEvent;
import com.smooth.drivecast_service.incident.dto.IncidentType;
import com.smooth.drivecast_service.incident.service.mapper.IncidentMappingContext;
import com.smooth.drivecast_service.incident.service.mapper.IncidentMessageMapperFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentRepeatNotifier {

    private final IncidentMessageMapperFactory incidentMessageMapperFactory;
    private final RealtimePublisher publisher;
    private final VicinityService vicinityService;
    private final DedupService dedupService;

    @Async
    public CompletableFuture<Void> startRepeatNotification(IncidentEvent event, String alertId) {
        log.info("반복 알림 시작: type={}, alertId={}, lat={}, lng={}", 
                event.type(), alertId, event.latitude(), event.longitude());

        try {
            for (int round = 1; round <= 18; round++) {
                Thread.sleep(10_000);
                sendNotificationRound(event, alertId, round);
            }

            log.info("반복 알림 완료: type={}, alertId={}", event.type(), alertId);
            return CompletableFuture.completedFuture(null);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("반복 알림 중단됨: type={}, alertId={}", event.type(), alertId);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("반복 알림 중 오류 발생: type={}, alertId={}", event.type(), alertId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private void sendNotificationRound(IncidentEvent event, String alertId, int round) {
        try {
            log.debug("반복 알림 {}차 전송 시작: type={}, alertId={}", round, event.type(), alertId);

            Instant currentTime = Instant.now();
            
            boolean excludeSelf = event.type() == IncidentType.ACCIDENT;
            List<String> nearbyUsers = vicinityService.findUsers(
                    event.latitude(),
                    event.longitude(),
                    event.type().getRadiusMeters(),
                    !excludeSelf,
                    30,
                    3,
                    List.of(100L, 200L, 500L),
                    currentTime,
                    excludeSelf ? event.userId() : null
            );

            if (nearbyUsers.isEmpty()) {
                log.debug("반복 알림 {}차: 반경 내 사용자 없음", round);
                return;
            }

            var mapper = incidentMessageMapperFactory.get(event.type().getValue());
            if (mapper.isEmpty()) {
                log.warn("지원하지 않는 사고 타입: {}", event.type());
                return;
            }

            int sentCount = 0;
            
            for (String userId : nearbyUsers) {
                try {
                    if (dedupService.markAlertIfFirst(alertId, userId)) {
                        var context = IncidentMappingContext.of(event, userId);
                        mapper.get().map(context).ifPresent(message -> {
                            publisher.toUser(userId, IncidentDestinations.INCIDENT_ALERT, message);
                            log.debug("반복 알림 {}차 전송 (새 진입자): userId={}, alertId={}", round, userId, alertId);
                        });
                        sentCount++;
                    }
                } catch (Exception e) {
                    log.warn("반복 알림 사용자별 전송 실패 (스킵): userId={}, alertId={}, round={}", userId, alertId, round, e);
                }
            }

            log.debug("반복 알림 {}차 전송 완료: type={}, 대상={}명, 새 진입자={}명", 
                    round, event.type(), nearbyUsers.size(), sentCount);

        } catch (Exception e) {
            log.error("반복 알림 {}차 전송 중 오류: type={}, alertId={}", round, event.type(), alertId, e);
        }
    }
}