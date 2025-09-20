package com.smooth.drivecast_service.driving.constants;

public class DrivingVicinityPolicy {

    public static final int WINDOW_SECONDS = 5;
    public static final int RADIUS_METERS = 15;

    public static final int FRESHNESS_SEC = 300;
    public static final java.time.Duration FRESHNESS_THRESHOLD = java.time.Duration.ofSeconds(FRESHNESS_SEC);

    public static final int MAX_RETRIES = 2;
    public static final int[] RETRY_DELAYS_MS = {120, 250};

    public static final int HOT_CACHE_TTL_SEC = 20;
    public static final int WARM_CACHE_TTL_SEC = 24 * 60 * 60;

    public static final String TRAIT_HOT_PREFIX = "trait:";
    public static final String TRAIT_WARM_PREFIX = "trait:warm:";
    public static final String DRIVING_ACTIVE_SET = "driving:active";

    public static final int ACTIVE_SET_TTL_SEC = 60 * 60;

    private DrivingVicinityPolicy() {}
}
