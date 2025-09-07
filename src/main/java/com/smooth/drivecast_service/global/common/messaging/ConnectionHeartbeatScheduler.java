package com.smooth.drivecast_service.global.common.messaging;

import com.smooth.drivecast_service.global.common.cache.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * WebSocket 연결 하트비트 스케줄러
 * Presence 정보와 연동하여 전역 연결 TTL 갱신
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConnectionHeartbeatScheduler {

    private final LocalConnectionManager localConnectionManager;
    private final GlobalConnectionManager globalConnectionManager;
    private final PresenceService presenceService;
    
    /**
     * 30초마다 활성 연결의 TTL 갱신
     */
    @Scheduled(fixedRate = 30000) // 30초
    public void refreshActiveConnections() {
        try {
            var activeUsers = localConnectionManager.getActiveConnections();
            if (activeUsers.isEmpty()) {
                log.debug("활성 연결 없음, 하트비트 스킵");
                return;
            }
            
            Instant now = Instant.now();
            int refreshedCount = 0;
            
            for (String userId : activeUsers) {
                try {
                    // Presence 기반 활성 상태 확인 (현재 시간 기준으로 체크)
                    var lastSeenOpt = presenceService.getLastSeen(userId);
                    Instant threshold = now.minus(Duration.ofMinutes(5));
                    
                    if (lastSeenOpt.isPresent() && lastSeenOpt.get().isAfter(threshold)) {
                        // 활성 사용자의 전역 연결 TTL 갱신
                        globalConnectionManager.refreshConnection(userId);
                        refreshedCount++;
                        
                        log.debug("연결 하트비트 갱신: userId={}", userId);
                    } else {
                        log.debug("비활성 사용자 하트비트 스킵: userId={}", userId);
                    }
                    
                } catch (Exception e) {
                    log.warn("사용자 하트비트 처리 실패: userId={}", userId, e);
                }
            }
            
            if (refreshedCount > 0) {
                log.debug("하트비트 갱신 완료: 활성={}명, 갱신={}명", activeUsers.size(), refreshedCount);
            }
            
        } catch (Exception e) {
            log.error("하트비트 스케줄러 오류", e);
        }
    }
}