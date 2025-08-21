package com.smooth.drivecast_service.core;

import com.smooth.drivecast_service.model.AlertEvent;
import com.smooth.drivecast_service.model.EventType;
import com.smooth.drivecast_service.repository.AlertCacheService;
import com.smooth.drivecast_service.support.util.AlertIdResolver;
import com.smooth.drivecast_service.support.util.KoreanTimeUtil;
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

import jakarta.annotation.PreDestroy;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertRepeatNotifier {

    private final VicinityUserFinder vicinityUserFinder;
    private final AlertCacheService alertCacheService;
    private final AlertSender alertSender;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
            2,
            new CustomizableThreadFactory("alert-repeat-")
    );

    public void start(AlertEvent event) {
        log.info("🔍 AlertRepeatNotifier.start 호출: type={}, userId={}, accidentId={}",
                event.type(), event.userId(), event.accidentId());

        Optional<String> alertIdOpt = AlertIdResolver.resolve(event);
        if(alertIdOpt.isEmpty()) {
            log.warn("❌ 반복 알림 제외 대상 - AlertId 없음: type={}, userId={}, accidentId={}",
                    event.type(), event.userId(), event.accidentId());
            return;
        }

        String alertId = alertIdOpt.get();
        log.info("✅ AlertId 생성 완료: alertId={}", alertId);

        int radius = EventType.from(event.type())
                .map(EventType::getRadiusMeters)
                .orElse(0);

        // 1차 조회: 사고 발생 시점의 정확한 timestamp 키 사용
        performInitialNotification(event, alertId, radius);

        // 2차 이후: 최신 키로 반복 조회
        scheduleRepeatedNotification(event, alertId, radius);
    }

    // 최초 전파: refTime = event.timestamp (파싱 실패 시 now)
    private void performInitialNotification(AlertEvent event, String alertId, int radius) {
        log.info("🚀 초기 알림 처리 시작: alertId={}, radius={}, lat={}, lng={}",
                alertId, radius, event.latitude(), event.longitude());

        try {
            // OBSTACLE의 경우 본인 제외, ACCIDENT의 경우 본인 포함
            String excludeUserId = "obstacle".equals(event.type()) ? event.userId() : null;
            log.info("🎯 제외 대상 사용자: excludeUserId={}", excludeUserId);

            Instant refTime;
            try {
                // 한국시 문자열을 Instant로 변환
                refTime = KoreanTimeUtil.parseKoreanTime(event.timestamp());
                log.info("⏰ 시간 파싱 성공: timestamp={}, refTime={}", event.timestamp(), refTime);
            } catch (Exception parseEx) {
                log.warn("⚠️ 한국시 timestamp 파싱 실패, 현재시각 사용: ts={}, alertId={}", event.timestamp(), alertId);
                refTime = Instant.now();
            }

            String locationKey = KoreanTimeUtil.toLocationKey(refTime);
            log.info("🗝️ 위치 키 생성: locationKey={}", locationKey);

            List<String> targetUsers = vicinityUserFinder.findUsersAround(
                    event.latitude(),
                    event.longitude(),
                    radius,
                    refTime,
                    Duration.ofSeconds(5),
                    excludeUserId
            );

            log.info("👥 반경 내 사용자 조회 결과: 총 {}명, users={}", targetUsers.size(), targetUsers);

            // ACCIDENT의 경우 본인에게 먼저 무조건 알림 전송
            if ("accident".equals(event.type()) && event.userId() != null) {
                log.info("🚨 본인 사고 알림 처리 시작: userId={}", event.userId());

                boolean isFirstForSelf = alertCacheService.markIfFirst(alertId, event.userId());
                log.info("📝 본인 중복 체크 결과: userId={}, isFirst={}", event.userId(), isFirstForSelf);

                if (isFirstForSelf) {
                    AlertMessageMapper.map(event, event.userId()).ifPresentOrElse(msg -> {
                        log.info("📨 본인 메시지 매핑 성공: userId={}, messageType={}", event.userId(), msg.type());
                        alertSender.sendToUser(event.userId(), msg);
                        log.info("✅ 본인 사고 알림 전송 완료: userId={}, alertId={}", event.userId(), alertId);
                    }, () -> {
                        log.warn("❌ 본인 메시지 매핑 실패: userId={}", event.userId());
                    });
                } else {
                    log.info("⏭️ 본인 알림 이미 전송됨: userId={}", event.userId());
                }
            }

            // 반경 내 다른 사용자들에게 알림 전송
            for (String userId : targetUsers) {
                log.info("🔄 사용자별 알림 처리 시작: userId={}, alertId={}", userId, alertId);

                boolean isFirst = alertCacheService.markIfFirst(alertId, userId);
                log.info("📝 중복 체크 결과: userId={}, isFirst={}, alertId={}", userId, isFirst, alertId);

                if (!isFirst) {
                    log.info("⏭️ 이미 전송된 알림 스킵: userId={}, alertId={}", userId, alertId);
                    continue;
                }

                AlertMessageMapper.map(event, userId).ifPresentOrElse(msg -> {
                    log.info("📨 메시지 매핑 성공: userId={}, messageType={}", userId, msg.type());
                    alertSender.sendToUser(userId, msg);

                    boolean isSelf = event.userId() != null && event.userId().equals(userId);
                    String msgType = isSelf ? "내사고" : ("obstacle".equals(event.type()) ? "장애물" : "반경내사고");
                    log.info("✅ 초기 알림 전송 완료: type={}, userId={}, msgType={}, alertId={}",
                            event.type(), userId, msgType, alertId);
                }, () -> {
                    log.warn("❌ 메시지 매핑 실패: userId={}, eventType={}", userId, event.type());
                });
            }
        } catch (Exception e) {
            log.error("💥 초기 알림 처리 중 오류 발생: alertId={}, event={}", alertId, event, e);
        }

        log.info("🏁 초기 알림 처리 완료: alertId={}", alertId);
    }

    // 반복 전파: refTime = now (3분간, 10초 간격), 본인 제외로 새 진입자만
    private void scheduleRepeatedNotification(AlertEvent event, String alertId, int radius) {
        Instant endTime = Instant.now().plus(Duration.ofMinutes(3));

        var scheduledFuture = scheduler.scheduleAtFixedRate(() -> {
            try {
                if (Instant.now().isAfter(endTime)) {
                    log.info("알림 반복 종료: alertId={}", alertId);
                    return;
                }

                // 반복 알림에서는 항상 본인 제외 (새 진입자만 대상)
                List<String> nearbyUsers = vicinityUserFinder.findUsersAround(
                        event.latitude(),
                        event.longitude(),
                        radius,
                        Instant.now(),
                        Duration.ofSeconds(5),
                        event.userId() // 반복에서는 본인 제외
                );

                for (String userId : nearbyUsers) {
                    if (!alertCacheService.markIfFirst(alertId, userId)) continue;

                    AlertMessageMapper.map(event, userId).ifPresent(msg -> {
                        alertSender.sendToUser(userId, msg);
                        log.info("🔄 반복 알림 전송 완료: type={}, userId={} (새 진입자), alertId={}",
                                event.type(), userId, alertId);
                    });
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

    @PreDestroy
    public void shutdown() {
        log.info("AlertRepeatNotifier 스케줄러 종료 시작");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("스케줄러가 5초 내에 종료되지 않아 강제 종료합니다");
                scheduler.shutdownNow();
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.error("스케줄러 강제 종료 실패");
                }
            }
            log.info("AlertRepeatNotifier 스케줄러 종료 완료");
        } catch (InterruptedException e) {
            log.warn("스케줄러 종료 중 인터럽트 발생");
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
