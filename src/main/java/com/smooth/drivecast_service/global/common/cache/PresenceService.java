package com.smooth.drivecast_service.global.common.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public interface PresenceService {

    void markSeen(String userId, Instant when);

    Optional<Instant> getLastSeen(String userId);

    boolean isFresh(String userId, Instant refTime, Duration skew);
}
