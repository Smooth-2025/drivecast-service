package com.smooth.drivecast_service.global.common.cache;

import java.time.Duration;

public interface DedupService {

    boolean markIfFirst(String key, Duration ttl);

    default boolean markAlertIfFirst(String alertId, String userId) {
        String key = "alert:" + alertId + ":" + userId;
        return markIfFirst(key, Duration.ofMinutes(3));
    }

    void storeAccidentInfo(String accidentId, String accidentData, Duration ttl);

    default void storeAccidentInfo(String accidentId, String accidentData) {
        storeAccidentInfo(accidentId, accidentData, Duration.ofHours(1));
    }

    String getAccidentInfo(String accidentId);
}
