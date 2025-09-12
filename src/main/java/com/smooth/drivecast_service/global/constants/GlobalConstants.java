package com.smooth.drivecast_service.global.constants;

import java.time.Duration;
import java.util.List;

public class GlobalConstants {

    public static class Scheduling {
        public static final int HEARTBEAT_INTERVAL_MS = 30000;
        public static final Duration PRESENCE_THRESHOLD = Duration.ofMinutes(5);
        public static final String CACHE_WARMUP_CRON = "0 */10 * * * *";
        public static final String TIMEZONE = "Asia/Seoul";
    }

    public static class cache {
        public static final Duration DEFAULT_DEDUP_TTL = Duration.ofMinutes(3);
        public static final Duration DEFAULT_ACCIDENT_TTL = Duration.ofHours(1);
    }

    public static class Redis {
        public static final String WEBSOCKET_CHANNEL = "websocket:message";
        public static final String KICK_CHANNEL = "ws:system:kick";
        public static final String CONNECTION_KEY_PREFIX = "ws:global:connection:";
        public static final String HEALTH_CHECK_KEY = "health:check";
        public static final Duration CONNECTION_TTL = Duration.ofMinutes(5);
        public static final Duration HEALTH_CHECK_TTL = Duration.ofSeconds(30);
    }

    public static class WebSocket {
        public static final long[] HEARTBEAT_VALUES = {30000, 30000};
        public static final int SESSION_TIMEOUT_SEC = 1800;
    }

    public static class Retry {
        public static final int MAX_RETRIES = 3;
        public static final List<Long> RETRY_DELAYS_MS = List.of(100L, 200L, 500L);
    }

    private GlobalConstants() {
        throw new UnsupportedOperationException("상수 클래스입니다.");
    }
}
