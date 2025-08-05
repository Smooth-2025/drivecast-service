package com.smooth.alert_service.scheduler;

import com.smooth.alert_service.core.AlertRepeatNotifier;
import com.smooth.alert_service.model.AlertEvent;
import com.smooth.alert_service.repository.AlertCacheService;
import com.smooth.alert_service.core.VicinityUserFinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class AlertRepeatNotifierTest {

    private VicinityUserFinder vicinityUserFinder;
    private AlertCacheService alertCacheService;
    private AlertRepeatNotifier notifier;

    @BeforeEach
    void setUp() {
        vicinityUserFinder = mock(VicinityUserFinder.class);
        alertCacheService = mock(AlertCacheService.class);
        notifier = new AlertRepeatNotifier(vicinityUserFinder, alertCacheService);
    }

    @Test
    void 알림이_3분동안_중복없이_전송된다() throws InterruptedException {
        // given
        var event = new AlertEvent("accident", "acc-123", "driver-1", 37.5, 126.9, "2025-08-05T20:00:00Z");

        when(vicinityUserFinder.findNearbyUsers(event))
                .thenReturn(List.of("userA", "userB"));
        when(alertCacheService.isAlreadySent("acc-123", "userA")).thenReturn(false);
        when(alertCacheService.isAlreadySent("acc-123", "userB")).thenReturn(false);

        // when
        notifier.start(event);

        // then (wait for scheduler to run at least once)
        Thread.sleep(1200); // allow time for scheduler to execute

        verify(alertCacheService, atLeastOnce()).isAlreadySent("acc-123", "userA");
        verify(alertCacheService, atLeastOnce()).markAsSent("acc-123", "userA");
        verify(alertCacheService, atLeastOnce()).markAsSent("acc-123", "userB");
    }


    @Test
    void 이미_전송된_사용자는_건너뛴다() throws InterruptedException {
        // given
        var event = new AlertEvent("accident", "acc-999", "driver-9", 37.5, 126.9, "2025-08-05T20:03:00Z");

        when(vicinityUserFinder.findNearbyUsers(event))
                .thenReturn(List.of("userX", "userY"));
        when(alertCacheService.isAlreadySent("acc-999", "userX")).thenReturn(true);
        when(alertCacheService.isAlreadySent("acc-999", "userY")).thenReturn(false);

        // when
        notifier.start(event);

        // then
        Thread.sleep(1200); // allow time for scheduler to execute

        verify(alertCacheService, never()).markAsSent("acc-999", "userX");
        verify(alertCacheService, atLeastOnce()).markAsSent("acc-999", "userY");
    }
}