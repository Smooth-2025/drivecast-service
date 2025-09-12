package com.smooth.drivecast_service.global.common.cache;

import java.time.Duration;

import static com.smooth.drivecast_service.global.constants.GlobalConstants.cache.DEFAULT_ACCIDENT_TTL;
import static com.smooth.drivecast_service.global.constants.GlobalConstants.cache.DEFAULT_DEDUP_TTL;

public interface DedupService {

    boolean markIfFirst(String key, Duration ttl);

    default boolean markAlertIfFirst(String alertId, String userId) {
        String key = "alert:" + alertId + ":" + userId;
        return markIfFirst(key, DEFAULT_DEDUP_TTL);
    }

    void storeAccidentInfo(String accidentId, String accidentData, Duration ttl);

    default void storeAccidentInfo(String accidentId, String accidentData) {
        storeAccidentInfo(accidentId, accidentData, DEFAULT_ACCIDENT_TTL);
    }

    String getAccidentInfo(String accidentId);
}
