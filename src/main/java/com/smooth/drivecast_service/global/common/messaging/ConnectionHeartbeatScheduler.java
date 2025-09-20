package com.smooth.drivecast_service.global.common.messaging;

import com.smooth.drivecast_service.global.common.cache.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConnectionHeartbeatScheduler {

    private final LocalConnectionManager localConnectionManager;
    private final GlobalConnectionManager globalConnectionManager;
    private final PresenceService presenceService;

    @Scheduled(fixedRate = 30000)
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
                    var lastSeenOpt = presenceService.getLastSeen(userId);
                    Instant threshold = now.minus(Duration.ofMinutes(5));
                    
                    if (lastSeenOpt.isPresent() && lastSeenOpt.get().isAfter(threshold)) {
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