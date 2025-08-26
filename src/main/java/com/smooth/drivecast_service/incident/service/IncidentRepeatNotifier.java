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

/**
 * 사고 이벤트 반복 알림 서비스
 * 기존 AlertRepeatNotifier 역할을 대체
 * 주요 기능:
 * - 사고 발생 시 주변 사용자들에게 반복 알림
 * - 중복 전송 방지
 * - 비동기 처리로 성능 최적화
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentRepeatNotifier {

    private final IncidentMessageMapperFactory incidentMessageMapperFactory;
    private final RealtimePublisher publisher;
    private final VicinityService vicinityService;
    private final DedupService dedupService;

    /**
     * 사고 이벤트 반복 알림 시작
     * @param event 사고 이벤트
     * @param alertId 알림 ID
     */
    @Async
    public CompletableFuture<Void> startRepeatNotification(IncidentEvent event, String alertId) {
        log.info("반복 알림 시작: type={}, alertId={}, lat={}, lng={}", 
                event.type(), alertId, event.latitude(), event.longitude());

        try {
            // 3분간 10초마다 반복 (총 18회)
            for (int round = 1; round <= 18; round++) {
                Thread.sleep(10_000); // 10초 대기
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

            // 현재 시각 기준으로 반경 내 사용자 검색 (새로운 진입자 탐지)
            Instant currentTime = Instant.now();
            
            boolean excludeSelf = event.type() == IncidentType.ACCIDENT;
            List<String> nearbyUsers = vicinityService.findUsers(
                    event.latitude(),
                    event.longitude(),
                    event.type().getRadiusMeters(),
                    !excludeSelf, // includeSelf = !excludeSelf
                    30, // 30초 내 활동한 사용자
                    3,  // 최대 3회 재시도
                    List.of(100L, 200L, 500L), // 재시도 지연
                    currentTime,
                    excludeSelf ? event.userId() : null // excludeUserId
            );

            if (nearbyUsers.isEmpty()) {
                log.debug("반복 알림 {}차: 반경 내 사용자 없음", round);
                return;
            }

            // 매퍼 준비
            var mapper = incidentMessageMapperFactory.get(event.type().getValue());
            if (mapper.isEmpty()) {
                log.warn("지원하지 않는 사고 타입: {}", event.type());
                return;
            }

            // 새로운 진입자에게만 알림 전송 (per-recipient 예외 처리)
            int sentCount = 0;
            
            for (String userId : nearbyUsers) {
                try {
                    // 이미 이 alertId로 알림을 받은 사용자는 제외
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
                    // 해당 사용자만 스킵하고 다음 사용자로 진행
                }
            }

            log.debug("반복 알림 {}차 전송 완료: type={}, 대상={}명, 새 진입자={}명", 
                    round, event.type(), nearbyUsers.size(), sentCount);

        } catch (Exception e) {
            log.error("반복 알림 {}차 전송 중 오류: type={}, alertId={}", round, event.type(), alertId, e);
        }
    }
}