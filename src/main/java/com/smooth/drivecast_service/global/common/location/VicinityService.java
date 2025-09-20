package com.smooth.drivecast_service.global.common.location;

import java.time.Instant;
import java.util.List;

public interface VicinityService {

    List<String> findUsers(double latitude, double longitude, int radiusMeters, boolean includeSelf, int freshnessSec, int maxRetries, List<Long> retryDelaysMs, Instant refTime, String excludeUserId);

    default List<String> findAllUsers(double latitude, double longitude, int radiusMeters, int freshnessSec, int maxRetries, List<Long> retryDelaysMs, Instant refTime) {
        return findUsers(latitude, longitude, radiusMeters, true, freshnessSec, maxRetries, retryDelaysMs, refTime, null);
    }

    List<String> searchNearby(String locationKey, double latitude, double longitude, int radiusMeters);

    boolean existsLocationKey(String locationKey);
}
