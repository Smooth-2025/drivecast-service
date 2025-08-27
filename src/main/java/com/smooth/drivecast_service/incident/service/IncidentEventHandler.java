package com.smooth.drivecast_service.incident.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smooth.drivecast_service.global.common.cache.DedupService;
import com.smooth.drivecast_service.global.common.location.VicinityService;
import com.smooth.drivecast_service.global.common.notification.RealtimePublisher;
import com.smooth.drivecast_service.global.exception.BusinessException;
import com.smooth.drivecast_service.global.util.IdGenerators;
import com.smooth.drivecast_service.global.util.KoreanTimeUtil;
import com.smooth.drivecast_service.incident.dto.IncidentDestinations;
import com.smooth.drivecast_service.incident.dto.IncidentEvent;
import com.smooth.drivecast_service.incident.exception.IncidentErrorCode;
import com.smooth.drivecast_service.incident.service.mapper.IncidentMappingContext;
import com.smooth.drivecast_service.incident.service.mapper.IncidentMessageMapperFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentEventHandler {

    private final IncidentRepeatNotifier incidentRepeatNotifier;
    private final IncidentMessageMapperFactory incidentMessageMapperFactory;
    private final RealtimePublisher publisher;
    private final VicinityService vicinityService;
    private final DedupService dedupService;
    private final ObjectMapper objectMapper;

    public void handle(IncidentEvent event) {
        log.info("사고 이벤트 처리 시작: type={}, userId={}, lat={}, lng={}, timestamp={}",
                event.type(), event.userId(), event.latitude(), event.longitude(), event.timestamp());

        try {
            var alertId = IdGenerators.generateIncidentAlertId(event);

            // 사고 정보 캐시 저장 추가
            storeAccidentInfo(event, alertId);
            // 1. 즉시 알림 전송
            sendImmediateNotifications(event, alertId);

            // 2. 반복 알림 시작 (비동기)
            incidentRepeatNotifier.startRepeatNotification(event, alertId);

            log.info("사고 이벤트 처리 완료: type={}, alertId={}", event.type(), alertId);
        } catch (Exception e) {
            log.error("사고 이벤트 처리 중 오류 발생: type={}, lat={}, lng={}",
                    event.type(), event.latitude(), event.longitude(), e);
            throw new BusinessException(IncidentErrorCode.INCIDENT_EVENT_PROCESSING_FAILED, e.getMessage());
        }
    }

    private void sendImmediateNotifications(IncidentEvent event, String alertId) {
        try {
            switch (event.type()) {
                case ACCIDENT -> sendAccidentNotifications(event, alertId);
                case OBSTACLE -> sendObstacleNotifications(event, alertId);
            }
        } catch (Exception e) {
            log.error("즉시 알림 전송 중 오류 발생: type={}, alertId={}", event.type(), alertId, e);
            throw new BusinessException(IncidentErrorCode.INCIDENT_NOTIFICATION_FAILED, e.getMessage());
        }
    }

    private void sendAccidentNotifications(IncidentEvent event, String alertId) {
        // 1. 본인에게 즉시 알림 (accident)
        if (event.userId() != null && !event.userId().isBlank()) {
            sendToSelf(event, alertId);
        }

        // 2. 반경 내 다른 운전자에게 즉시 알림 (accident-nearby)
        sendToNearbyUsers(event, alertId, true); // excludeSelf = true
    }

    private void sendObstacleNotifications(IncidentEvent event, String alertId) {
        // 반경 내 모든 운전자에게 즉시 알림 (obstacle)
        sendToNearbyUsers(event, alertId, false); // excludeSelf = false
    }

    private void sendToSelf(IncidentEvent event, String alertId) {
        try {
            var context = IncidentMappingContext.of(event, event.userId());
            var mapper = incidentMessageMapperFactory.get(event.type().getValue());

            if (mapper.isEmpty()) {
                log.warn("지원하지 않는 사고 타입: {}", event.type());
                return;
            }

            // 중복 방지
            if (dedupService.markAlertIfFirst(alertId, event.userId())) {
                mapper.get().map(context).ifPresent(message -> {
                    publisher.toUser(event.userId(), IncidentDestinations.INCIDENT_ALERT, message);
                    log.info("본인 사고 알림 전송: type={}, userId={}, alertId={}", 
                            event.type(), event.userId(), alertId);
                });
            }
        } catch (Exception e) {
            log.error("본인 알림 전송 중 오류: userId={}, alertId={}", event.userId(), alertId, e);
        }
    }

    private void sendToNearbyUsers(IncidentEvent event, String alertId, boolean excludeSelf) {
        try {
            // 반경 내 사용자 검색
            Instant refTime = KoreanTimeUtil.parseKoreanTime(event.timestamp());
            List<String> nearbyUsers = vicinityService.findUsers(
                    event.latitude(),
                    event.longitude(),
                    event.type().getRadiusMeters(),
                    !excludeSelf, // includeSelf = !excludeSelf
                    30, // 30초 내 활동한 사용자
                    3,  // 최대 3회 재시도
                    List.of(100L, 200L, 500L), // 재시도 지연
                    refTime,
                    excludeSelf ? event.userId() : null // excludeUserId
            );

            if (nearbyUsers.isEmpty()) {
                log.info("반경 내 사용자 없음: type={}, alertId={}", event.type(), alertId);
                return;
            }

            // 매퍼 준비
            var mapper = incidentMessageMapperFactory.get(event.type().getValue());
            if (mapper.isEmpty()) {
                log.warn("지원하지 않는 사고 타입: {}", event.type());
                return;
            }

            // 각 사용자에게 알림 전송 (per-recipient 예외 처리)
            int sentCount = 0;
            for (String userId : nearbyUsers) {
                try {
                    if (dedupService.markAlertIfFirst(alertId, userId)) {
                        var context = IncidentMappingContext.of(event, userId);
                        mapper.get().map(context).ifPresent(message -> {
                            publisher.toUser(userId, IncidentDestinations.INCIDENT_ALERT, message);
                            log.debug("반경 내 즉시 알림 전송: type={}, userId={}, alertId={}", 
                                    event.type(), userId, alertId);
                        });
                        sentCount++;
                    }
                } catch (Exception e) {
                    log.warn("사용자별 알림 전송 실패 (스킵): userId={}, alertId={}", userId, alertId, e);
                    // 해당 사용자만 스킵하고 다음 사용자로 진행
                }
            }

            log.info("반경 내 즉시 알림 전송 완료: type={}, alertId={}, 대상={}명, 전송={}명",
                    event.type(), alertId, nearbyUsers.size(), sentCount);

        } catch (Exception e) {
            log.error("반경 내 알림 전송 중 오류: type={}, alertId={}", event.type(), alertId, e);
        }
    }

    private void storeAccidentInfo(IncidentEvent event, String alertId) {
        try {
            String accidentJson = objectMapper.writeValueAsString(event);
            dedupService.storeAccidentInfo(alertId, accidentJson);
            log.info("사고 정보 저장 완료: alertId={}, type={}", alertId, event.type());
        } catch (Exception e) {
            log.error("사고 정보 저장 실패: alertId={}, type={}", alertId, event.type(), e);
        }
    }
}
